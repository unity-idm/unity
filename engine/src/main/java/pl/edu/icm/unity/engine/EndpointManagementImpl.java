/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.authn.AuthenticatorImpl;
import pl.edu.icm.unity.engine.internal.EngineHelper;
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
	private static final Logger log = Logger.getLogger(EndpointManagement.class);
	private EndpointFactoriesRegistry endpointFactoriesReg;
	private DBSessionManager db;
	private DBGeneric dbGeneric;
	private JettyServer httpServer;
	private InternalEndpointManagement internalManagement;
	private EngineHelper engineHelper;
	
	@Autowired
	public EndpointManagementImpl(EndpointFactoriesRegistry endpointFactoriesReg,
			DBSessionManager db, DBGeneric dbGeneric, JettyServer httpServer,
			InternalEndpointManagement internalManagement,
			EngineHelper engineHelper)
	{
		super();
		this.endpointFactoriesReg = endpointFactoriesReg;
		this.db = db;
		this.dbGeneric = dbGeneric;
		this.httpServer = httpServer;
		this.internalManagement = internalManagement;
		this.engineHelper = engineHelper;
	}



	@Override
	public List<EndpointTypeDescription> getEndpointTypes()
	{
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
	public synchronized EndpointDescription deploy(String typeId, String endpointName, String address, 
			String jsonConfiguration) throws EngineException 
	{
		EndpointFactory factory = endpointFactoriesReg.getById(typeId);
		if (factory == null)
			throw new IllegalArgumentException("Endpoint type " + typeId + " is unknown");
		EndpointInstance instance = factory.newInstance();
		if (!(instance instanceof WebAppEndpointInstance))
			throw new RuntimeEngineException("Endpoint type " + typeId + " provides endpoint of " + 
					instance.getClass() + " class, which is unsupported.");
		instance.configure(address, jsonConfiguration);
		instance.setId(endpointName);
		
		SqlSession sql = db.getSqlSession(true);
		try
		{
			byte[] contents = instance.getSerializedConfiguration().getBytes(Constants.UTF); 
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
		List<WebAppEndpointInstance> endpoints = httpServer.getDeployedEndpoints();
		List<EndpointDescription> ret = new ArrayList<EndpointDescription>(endpoints.size());
		for (WebAppEndpointInstance endpI: endpoints)
			ret.add(endpI.getEndpointDescription());
		return ret;
	}

	@Override
	public synchronized void undeploy(String id) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean inDb = dbGeneric.getObjectByNameType(id, 
					InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, sql);
			if (inDb == null)
				throw new IllegalArgumentException("There is no endpoint with the provided id");
			WebAppEndpointInstance endpoint = getDeployedInstance(id);
			//if endpoint is null, it means that we are in a situation where endpoint was added to database
			//out of bands (typically in effect of synchronization in redundant DB deployment)
			//and this change was not yet picked up by this server runtime. So we can simply skip undeployment
			//from the runtime.
			if (endpoint != null)
			{
				httpServer.undeployEndpoint(id);
				try
				{
					endpoint.destroy();
				} catch (Exception e)
				{
					log.error("The destroy operation failed on a removed endpoint instance. " +
							"The removal will continue nevertheless.", e);
				}
			}
			dbGeneric.removeObject(id, InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, sql);
			sql.commit();
		} catch (Exception e)
		{
			throw new EngineException("Unable to deploy an endpoint: " + e.getMessage(), e);
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	@Override
	public synchronized void updateEndpoint(String id, String description, String jsonConfiguration, 
			List<AuthenticatorSet> authn) throws EngineException
	{
		SqlSession sql = db.getSqlSession(true);
		try
		{
			GenericObjectBean inDb = dbGeneric.getObjectByNameType(id, 
					InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, sql);
			if (inDb == null)
				throw new IllegalArgumentException("There is no endpoint with the id " + id);
			WebAppEndpointInstance endpoint = getDeployedInstance(id);
			//if endpoint is null, it means that we are in a situation where endpoint was added to database
			//out of bands (typically in effect of synchronization in redundant DB deployment)
			//and this change was not yet picked up by this server runtime. So we can simply skip 
			//reconfiguration of the runtime instance - only DB needs to be updated, and the changes will
			//be picked up from it.
			if (endpoint != null)
			{
				if (jsonConfiguration != null)
					endpoint.configure(endpoint.getServletContextHandler().getContextPath(), 
							jsonConfiguration);
				if (authn != null)
				{
					List<Map<String, BindingAuthn>> authenticators = getAuthenticators(authn, sql);
					endpoint.setAuthenticators(authn, authenticators);
				}
				if (description != null)
					endpoint.setDescription(description);
			} else
			{
				endpoint = (WebAppEndpointInstance) internalManagement.recreateEndpointFromDB(
						inDb.getId()+"", inDb.getName(), inDb.getContents());
			}
			byte[] contents = endpoint.getSerializedConfiguration().getBytes(Constants.UTF); 
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
	}

	private List<Map<String, BindingAuthn>> getAuthenticators(List<AuthenticatorSet> authn, SqlSession sql)
	{
		List<Map<String, BindingAuthn>> ret = new ArrayList<Map<String, BindingAuthn>>(authn.size());
		for (AuthenticatorSet aSet: authn)
		{
			Set<String> authenticators = aSet.getAuthenticators();
			Map<String, BindingAuthn> aImpls = new HashMap<String, BindingAuthn>();
			for (String authenticator: authenticators)
			{
				AuthenticatorImpl authImpl = engineHelper.getAuthenticator(authenticator, sql);
				aImpls.put(authenticator, authImpl.getRetrieval());
			}
			ret.add(aImpls);
		}
		return ret;
	}
	
	private WebAppEndpointInstance getDeployedInstance(String id)
	{
		List<WebAppEndpointInstance> endpoints = httpServer.getDeployedEndpoints();
		for (WebAppEndpointInstance deployedApp: endpoints)
			if (deployedApp.getEndpointDescription().getId().equals(id))
			{
				return deployedApp;
			}
		return null;
	}
}
