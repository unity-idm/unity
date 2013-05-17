/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.internal.EndpointsUpdater;
import pl.edu.icm.unity.engine.internal.InternalEndpointManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.registries.EndpointFactoriesRegistry;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Implementation of the endpoint management. Currently only web application endpoints are supported.
 * @author K. Benedyczak
 */
@Component
public class EndpointManagementImpl implements EndpointManagement
{
	private EndpointFactoriesRegistry endpointFactoriesReg;
	private DBSessionManager db;
	private DBGeneric dbGeneric;
	private JettyServer httpServer;
	private InternalEndpointManagement internalManagement;
	private EndpointsUpdater endpointsUpdater;
	private AuthorizationManager authz;
	
	@Autowired
	public EndpointManagementImpl(EndpointFactoriesRegistry endpointFactoriesReg,
			DBSessionManager db, DBGeneric dbGeneric, JettyServer httpServer,
			InternalEndpointManagement internalManagement,
			EndpointsUpdater endpointsUpdater, AuthorizationManager authz)
	{
		this.endpointFactoriesReg = endpointFactoriesReg;
		this.db = db;
		this.dbGeneric = dbGeneric;
		this.httpServer = httpServer;
		this.internalManagement = internalManagement;
		this.endpointsUpdater = endpointsUpdater;
		this.authz = authz;
	}



	@Override
	public List<EndpointTypeDescription> getEndpointTypes()
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		return endpointFactoriesReg.getDescriptions();
	}

	/**
	 * Deploys an endpoint as follows:
	 *  {@link EndpointFactory} is searched in registry by id
	 *  It is used to create a new endpoint instance
	 *  the instance is configured with JSON and context address
	 *  transaction is started
	 *  instance state is added to DB
	 *  instance is deployed into web server
	 *  transaction is committed
	 */
	@Override
	public EndpointDescription deploy(String typeId, String endpointName, String address, String description,
			List<AuthenticatorSet> authn, String jsonConfiguration) throws EngineException 
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		synchronized(internalManagement)
		{
			return deployInt(typeId, endpointName, address, description, authn, jsonConfiguration);
		}
	}

	private EndpointDescription deployInt(String typeId, String endpointName, String address, String description,
			List<AuthenticatorSet> authenticatorsInfo, String jsonConfiguration) throws EngineException 
	{
		EndpointFactory factory = endpointFactoriesReg.getById(typeId);
		if (factory == null)
			throw new IllegalArgumentException("Endpoint type " + typeId + " is unknown");
		EndpointInstance instance = factory.newInstance();
		if (!(instance instanceof WebAppEndpointInstance))
			throw new RuntimeEngineException("Endpoint type " + typeId + " provides endpoint of " + 
					instance.getClass() + " class, which is unsupported.");
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<Map<String, BindingAuthn>> authenticators = internalManagement.getAuthenticators(authenticatorsInfo, sql);
			instance.initialize(endpointName, httpServer.getUrls()[0], 
					address, description, authenticatorsInfo, authenticators, jsonConfiguration);

			byte[] contents = internalManagement.serializeEndpoint(instance); 
			dbGeneric.addObject(endpointName, InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, 
					typeId, contents, sql);
			httpServer.deployEndpoint((WebAppEndpointInstance) instance);
			sql.commit();
		} catch (Exception e)
		{
			instance.destroy();
			throw new EngineException("Unable to deploy an endpoint: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
		return instance.getEndpointDescription();
	}
	
	@Override
	public List<EndpointDescription> getEndpoints()
	{
		authz.checkAuthorization(AuthzCapability.readInfo);
		List<WebAppEndpointInstance> endpoints = httpServer.getDeployedEndpoints();
		List<EndpointDescription> ret = new ArrayList<EndpointDescription>(endpoints.size());
		for (WebAppEndpointInstance endpI: endpoints)
			ret.add(endpI.getEndpointDescription());
		return ret;
	}

	@Override
	public void undeploy(String id) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		synchronized(internalManagement)
		{
			undeployInt(id);
		}
	}

	private void undeployInt(String id) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean inDb = dbGeneric.getObjectByNameType(id, 
					InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, sql);
			if (inDb == null)
				throw new IllegalArgumentException("There is no endpoint with the provided id");
			dbGeneric.removeObject(id, InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, sql);
			sql.commit();
		} catch (Exception e)
		{
			throw new EngineException("Unable to deploy an endpoint: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
		endpointsUpdater.updateEndpoints();
	}	
	
	@Override
	public void updateEndpoint(String id, String description, List<AuthenticatorSet> authn,
			String jsonConfiguration) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		synchronized(internalManagement)
		{
			updateEndpointInt(id, description, jsonConfiguration, authn);
		}
	}

	/**
	 * -) Get from DB
	 * -) create a new instance
	 * -) in the new instance set all fields to the new ones if not null or to the existing values
	 * -) serialize and store in db
	 * -) trigger runtime system update.
	 * @param id
	 * @param description
	 * @param jsonConfiguration
	 * @param authn
	 * @throws EngineException
	 */
	private void updateEndpointInt(String id, String description, String jsonConfiguration, 
			List<AuthenticatorSet> authn) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean inDb = dbGeneric.getObjectByNameType(id, 
					InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, sql);
			if (inDb == null)
				throw new IllegalArgumentException("There is no endpoint with the id " + id);
			EndpointInstance instance = internalManagement.deserializeEndpoint(id, 
					inDb.getSubType(), inDb.getContents(), sql);
			
			EndpointFactory factory = endpointFactoriesReg.getById(inDb.getSubType());
			EndpointInstance newInstance = factory.newInstance();
			
			String jsonConf = (jsonConfiguration != null) ? jsonConfiguration : 
				instance.getSerializedConfiguration();
			String newDesc = (description != null) ? description : 
				instance.getEndpointDescription().getDescription();
			
			List<Map<String, BindingAuthn>> authenticators;
			List<AuthenticatorSet> newAuthn;
			if (authn != null)
			{
				newAuthn = authn;
				authenticators = internalManagement.getAuthenticators(authn, sql);
			} else
			{
				newAuthn = instance.getEndpointDescription().getAuthenticatorSets();
				authenticators = internalManagement.getAuthenticators(newAuthn, sql);
			}

			newInstance.initialize(id, httpServer.getUrls()[0], 
					instance.getEndpointDescription().getContextAddress(), 
					newDesc, newAuthn, authenticators, jsonConf);
			
			byte[] contents = internalManagement.serializeEndpoint(newInstance);
			dbGeneric.updateObject(inDb.getName(), InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, 
					contents, sql);

			sql.commit();				
		} catch (Exception e)
		{
			throw new EngineException("Unable to reconfigure an endpoint: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
		endpointsUpdater.updateEndpoints();
	}
}
