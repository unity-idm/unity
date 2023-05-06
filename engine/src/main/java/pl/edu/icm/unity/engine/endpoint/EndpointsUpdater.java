/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.utils.ScheduledUpdaterBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.generic.RealmDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.Endpoint.EndpointState;


/**
 * Allows for scanning the DB endpoints state. If it is detected during the scan that runtime configuration 
 * is outdated wrt DB contents, then the reconfiguration is done: existing endpoints are undeployed,
 * and redeployed from configuration.
 * <p>
 * To ensure synchronization this class is used by two components: periodically (to refresh state changes 
 * by another Unity instance) and manually by endpoints management (to refresh the state after local changes 
 * without waiting for the periodic update).
 * 
 * @author K. Benedyczak
 */
@Component
public class EndpointsUpdater extends ScheduledUpdaterBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, EndpointsUpdater.class);
	private InternalEndpointManagement endpointMan;
	private EndpointDB endpointDB;
	private RealmDB realmDB;
	private AuthenticatorConfigurationDB authnDB;
	private AuthenticationFlowDB authnFlowDB;
	private EndpointInstanceLoader loader;
	private TransactionalRunner tx;
	
	@Autowired
	public EndpointsUpdater(TransactionalRunner tx,
			InternalEndpointManagement endpointMan, EndpointDB endpointDB,
			AuthenticatorConfigurationDB authnDB, AuthenticationFlowDB authnFlowDB, EndpointInstanceLoader loader, RealmDB realmDB)
	{
		super("endpoints");
		this.tx = tx;
		this.endpointMan = endpointMan;
		this.endpointDB = endpointDB;
		this.authnDB = authnDB;
		this.loader = loader;
		this.authnFlowDB = authnFlowDB;
		this.realmDB = realmDB;
	}

	@Override
	protected void updateInternal() throws EngineException
	{
		List<EndpointInstance> deployedEndpoints = endpointMan.getDeployedEndpoints();
		Set<String> endpointsInDb = new HashSet<>();
		Map<String, EndpointInstance> endpointsDeployed = new HashMap<>();
		for (EndpointInstance endpoint: deployedEndpoints)
			endpointsDeployed.put(endpoint.getEndpointDescription().getName(), endpoint);
		log.debug("Running periodic endpoints update task. There are " + deployedEndpoints.size() + 
				" deployed endpoints.");
		try
		{
			tx.runInTransactionThrowing(() ->
			{
				long roundedUpdateTime = roundToS(System.currentTimeMillis());
				List<Endpoint> endpointsInDBMap = endpointDB.getAll();
				log.debug("There are " + endpointsInDBMap.size() + " endpoints in DB.");
				for (Endpoint endpointInDB : endpointsInDBMap)
				{
					if (endpointInDB.getState().equals(EndpointState.UNDEPLOYED))
						continue;
					EndpointInstance instance = updateEndpoint(endpointInDB, endpointsDeployed);
					endpointsInDb.add(instance.getEndpointDescription().getName());
				}
				setLastUpdate(roundedUpdateTime);

				undeployInactive(endpointsInDb, deployedEndpoints);
			});
		} catch (EndpointConfigurationException e)
		{
			log.error("Can not update endpoint", e);
			undeployAndChangeStateToUndeployedWhenInvalidConfiguration(e.endpoint);
			throw e.exception;
		}
	}
	
	private EndpointInstance updateEndpoint(Endpoint endpointInDB, Map<String, EndpointInstance> endpointsDeployed) throws EngineException
	{
		String name = endpointInDB.getName();
		EndpointInstance runtimeEndpointInstance = endpointsDeployed.get(name);
		EndpointInstance updatedInstance = null;
		if (runtimeEndpointInstance == null)
		{
			updatedInstance = createEndpointInstance(endpointInDB);
			log.info("Endpoint " + name + " will be deployed");
			endpointMan.deploy(updatedInstance);
		} else if (endpointInDB.getRevision() > runtimeEndpointInstance.getEndpointDescription()
				.getEndpoint().getRevision() || hasChangedRealm(runtimeEndpointInstance))
		{
			updatedInstance = createEndpointInstance(endpointInDB);
			log.info("Endpoint " + name + " will be re-deployed");
			endpointMan.undeploy(name);
			endpointMan.deploy(updatedInstance);
		} else if (hasChangedAuthenticationFlow(runtimeEndpointInstance))
		{
			updatedInstance = createEndpointInstance(endpointInDB);
			updateEndpointAuthenticators(name, updatedInstance, endpointsDeployed);
		} else if (hasChangedAuthenticator(runtimeEndpointInstance))
		{
			updatedInstance = createEndpointInstance(endpointInDB);
			updateEndpointAuthenticators(name, updatedInstance, endpointsDeployed);
		} 
		
		return updatedInstance == null ? runtimeEndpointInstance : updatedInstance;
	}
	
	
	private EndpointInstance createEndpointInstance(Endpoint endpointInDB) throws EndpointConfigurationException
	{
		try
		{
			return loader.createEndpointInstance(endpointInDB);
		} catch (ConfigurationException e)
		{
			throw new EndpointConfigurationException(endpointInDB, e);
		}
	}
	private void undeployAndChangeStateToUndeployedWhenInvalidConfiguration(Endpoint endpointInDB) throws EngineException
	{	
		tx.runInTransactionThrowing(() ->
		{
			endpointMan.undeploy(endpointInDB.getName());
			Endpoint updatedEndpoint = new Endpoint(endpointInDB.getName(), endpointInDB.getTypeId(),
					endpointInDB.getContextAddress(), endpointInDB.getConfiguration(), endpointInDB.getRevision() + 1,
					EndpointState.UNDEPLOYED);
			endpointDB.update(updatedEndpoint);			
		});
	}
	
	private void updateEndpointAuthenticators(String name, EndpointInstance instance,
			Map<String, EndpointInstance> endpointsDeployed) throws EngineException
	{
		log.info("Endpoint " + name + " will have its authenticators updated");
		EndpointInstance toUpdate = endpointsDeployed.get(name);
		try
		{
			toUpdate.updateAuthenticationFlows(instance.getAuthenticationFlows());
		} catch (UnsupportedOperationException e)
		{
			log.info("Endpoint " + name + " doesn't support authenticators update so will be redeployed");
			endpointMan.undeploy(instance.getEndpointDescription().getEndpoint().getName());
			endpointMan.deploy(instance);
		}
	}
	
	private boolean hasChangedRealm(EndpointInstance endpointInstance)
	{
		AuthenticationRealm currentRealm = endpointInstance.getEndpointDescription()
				.getRealm();
		if (currentRealm == null)
			return false;
		AuthenticationRealm dbRealm = realmDB.get(currentRealm.getName());
		return !dbRealm.equals(currentRealm);
	}
	
	/** 
	 * @param instance
	 * @return true if one of authenticator from instance is changed 
	 */
	private boolean hasChangedAuthenticator(EndpointInstance instance)
	{
		Map<String, Long> revisionMap = new HashMap<>();

		for (AuthenticationFlow flow : instance.getAuthenticationFlows())
		{
			for (AuthenticatorInstance authenticator : flow.getAllAuthenticators())
			{
				revisionMap.put(authenticator.getRetrieval().getAuthenticatorId(),
						authenticator.getRevision());
			}
		}

		Map<String, AuthenticatorConfiguration> allAuth = authnDB.getAllAsMap();

		for (String authn : revisionMap.keySet())
		{
			AuthenticatorConfiguration authInstance = allAuth.get(authn);
			if (authInstance != null)
			{
				if (authInstance.getRevision() > revisionMap.get(authn))
				{
					return true;
				}
			}
		}

		return false;

	}
	
	/**
	 * @param instance
	 * @return true if one of authentication flow from instance is changed 
	 */
	private boolean hasChangedAuthenticationFlow(EndpointInstance instance)
	{
		Map<String, AuthenticationFlowDefinition> all = authnFlowDB.getAllAsMap();
		for (AuthenticationFlow flow : instance.getAuthenticationFlows())
		{
			AuthenticationFlowDefinition dbFlowDefinition = all.get(flow.getId());
			
			if (dbFlowDefinition != null)
			{
				if (dbFlowDefinition.getRevision() > flow.getRevision())
					return true;
			}
		}	
		return false;
	}


	private void undeployInactive(Set<String> endpointsInDb, Collection<EndpointInstance> deployedEndpoints) 
			throws EngineException
	{
		for (EndpointInstance endpoint: deployedEndpoints)
		{
			String name = endpoint.getEndpointDescription().getName();
			if (!endpointsInDb.contains(name))
			{
				log.info("Undeploying endpoint: " + name);
				endpointMan.undeploy(name);
			}
		}
	}
	
	private static class EndpointConfigurationException extends EngineException
	{
		final Endpoint endpoint;
		final ConfigurationException exception;
		EndpointConfigurationException(Endpoint endpoint, ConfigurationException exception)
		{
			this.endpoint = endpoint;
			this.exception = exception;
		}	
	}
}
