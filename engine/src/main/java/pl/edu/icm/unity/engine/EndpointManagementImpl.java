/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.realm.RealmDB;
import pl.edu.icm.unity.engine.authn.AuthenticatorLoader;
import pl.edu.icm.unity.engine.authz.AuthorizationManager;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.endpoints.EndpointDB;
import pl.edu.icm.unity.engine.endpoints.EndpointsUpdater;
import pl.edu.icm.unity.engine.endpoints.InternalEndpointManagement;
import pl.edu.icm.unity.engine.events.InvocationEventProducer;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.engine.transactions.Transactional;
import pl.edu.icm.unity.engine.transactions.TransactionalRunner;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.server.endpoint.EndpointFactory;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.registries.EndpointFactoriesRegistry;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Implementation of the endpoint management. Currently only web application endpoints are supported.
 * @author K. Benedyczak
 */
@Component
@InvocationEventProducer
public class EndpointManagementImpl implements EndpointManagement
{
	private EndpointFactoriesRegistry endpointFactoriesReg;
	private AuthenticatorLoader authnLoader;
	private InternalEndpointManagement internalManagement;
	private EndpointsUpdater endpointsUpdater;
	private AuthorizationManager authz;
	private EndpointDB endpointDB;
	private RealmDB realmDB;
	private TransactionalRunner tx;
	
	@Autowired
	public EndpointManagementImpl(EndpointFactoriesRegistry endpointFactoriesReg,
			TransactionalRunner tx, AuthenticatorLoader authnLoader,
			InternalEndpointManagement internalManagement,
			EndpointsUpdater endpointsUpdater, AuthorizationManager authz,
			EndpointDB endpointDB, RealmDB realmDB)
	{
		this.endpointFactoriesReg = endpointFactoriesReg;
		this.tx = tx;
		this.authnLoader = authnLoader;
		this.internalManagement = internalManagement;
		this.endpointsUpdater = endpointsUpdater;
		this.authz = authz;
		this.endpointDB = endpointDB;
		this.realmDB = realmDB;
	}



	@Override
	public List<EndpointTypeDescription> getEndpointTypes() throws AuthorizationException
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
	@Transactional
	public EndpointDescription deploy(String typeId, String endpointName, I18nString displayedName, 
			String address, String description,
			List<AuthenticationOptionDescription> authn, String jsonConfiguration, String realm) throws EngineException 
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		synchronized(internalManagement)
		{
			return deployInt(typeId, endpointName, displayedName, address, description, authn, 
					realm, jsonConfiguration);
		}
	}

	private EndpointDescription deployInt(String typeId, String endpointName, I18nString displayedName, 
			String address, String description, List<AuthenticationOptionDescription> authenticatorsInfo, 
			String realmName, String jsonConfiguration) throws EngineException 
	{
		EndpointFactory factory = endpointFactoriesReg.getById(typeId);
		if (factory == null)
			throw new WrongArgumentException("Endpoint type " + typeId + " is unknown");
		EndpointInstance instance = factory.newInstance();
		SqlSession sql = SqlSessionTL.get();
		try
		{
			List<AuthenticationOption> authenticators = authnLoader.getAuthenticators(
					authenticatorsInfo, sql);
			verifyAuthenticators(authenticators, factory.getDescription().getSupportedBindings());
			AuthenticationRealm realm = realmDB.get(realmName, sql);
			
			EndpointDescription endpDescription = new EndpointDescription(
					endpointName, displayedName, address, description, realm, 
					factory.getDescription(), authenticatorsInfo);
			
			instance.initialize(endpDescription, authenticators, jsonConfiguration);
			endpointDB.insert(endpointName, instance, sql);
			internalManagement.deploy(instance);
		} catch (Exception e)
		{
			internalManagement.undeploy(instance.getEndpointDescription().getId());
			throw new EngineException("Unable to deploy an endpoint: " + e.getMessage(), e);
		}
		return instance.getEndpointDescription();
	}
	
	private void verifyAuthenticators(List<AuthenticationOption> authenticators, 
			Set<String> supported) throws WrongArgumentException
	{
		for (AuthenticationOption auths: authenticators)
			auths.checkIfAuthenticatorsAreAmongSupported(supported);
	}
	
	@Override
	public List<EndpointDescription> getEndpoints() throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		List<EndpointInstance> endpoints = internalManagement.getDeployedEndpoints();
		List<EndpointDescription> ret = new ArrayList<EndpointDescription>(endpoints.size());
		for (EndpointInstance endpI: endpoints)
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
		tx.runInTransaciton(() -> {
			try
			{
				endpointDB.remove(id, SqlSessionTL.get());
			} catch (Exception e)
			{
				throw new EngineException("Unable to undeploy an endpoint: " + e.getMessage(), e);
			}
		});
		endpointsUpdater.updateEndpoints();
	}	
	
	@Override
	public void updateEndpoint(String id, I18nString displayedName, String description, 
			List<AuthenticationOptionDescription> authn, String jsonConfiguration, String realm) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		synchronized(internalManagement)
		{
			updateEndpointInt(id, displayedName, description, jsonConfiguration, authn, realm);
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
	private void updateEndpointInt(String id, I18nString displayedName, String description, 
			String jsonConfiguration, 
			List<AuthenticationOptionDescription> authn, String realmName) throws EngineException
	{
		tx.runInTransaciton(() -> {
			SqlSession sql = SqlSessionTL.get();
			try
			{
				EndpointInstance instance = endpointDB.get(id, sql); 
				String endpointTypeId = instance.getEndpointDescription().getType().getName();
				EndpointFactory factory = endpointFactoriesReg.getById(endpointTypeId);
				EndpointInstance newInstance = factory.newInstance();
				
				String jsonConf = (jsonConfiguration != null) ? jsonConfiguration : 
					instance.getSerializedConfiguration();
				String newDesc = (description != null) ? description : 
					instance.getEndpointDescription().getDescription();
				
				List<AuthenticationOption> authenticators;
				List<AuthenticationOptionDescription> newAuthn;
				if (authn != null)
				{
					newAuthn = authn;
					authenticators = authnLoader.getAuthenticators(authn, sql);
				} else
				{
					newAuthn = instance.getEndpointDescription().getAuthenticatorSets();
					authenticators = authnLoader.getAuthenticators(newAuthn, sql);
				}
				AuthenticationRealm realm = realmDB.get(realmName, sql);
				
				EndpointDescription endpDescription = new EndpointDescription(
						id, displayedName, 
						instance.getEndpointDescription().getContextAddress(), 
						newDesc, realm, 
						factory.getDescription(), newAuthn);
				
				newInstance.initialize(endpDescription, authenticators, jsonConf);
				endpointDB.update(id, newInstance, sql);
			} catch (Exception e)
			{
				throw new EngineException("Unable to reconfigure an endpoint: " + e.getMessage(), e);
			}

		});
		endpointsUpdater.updateEndpointsManual();
	}
}
