/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoint;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.utils.ScheduledUpdaterBase;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorInstanceDB;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.endpoint.Endpoint;


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
	private static final Logger log = Log.getLogger(Log.U_SERVER, EndpointsUpdater.class);
	private InternalEndpointManagement endpointMan;
	private EndpointDB endpointDB;
	private AuthenticatorInstanceDB authnDB;
	private AuthenticationFlowDB authnFlowDB;
	private EndpointInstanceLoader loader;
	private TransactionalRunner tx;
	
	@Autowired
	public EndpointsUpdater(TransactionalRunner tx,
			InternalEndpointManagement endpointMan, EndpointDB endpointDB,
			AuthenticatorInstanceDB authnDB, AuthenticationFlowDB authnFlowDB, EndpointInstanceLoader loader)
	{
		super("endpoints");
		this.tx = tx;
		this.endpointMan = endpointMan;
		this.endpointDB = endpointDB;
		this.authnDB = authnDB;
		this.loader = loader;
		this.authnFlowDB = authnFlowDB;
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
		
		tx.runInTransactionThrowing(() -> {
			long roundedUpdateTime = roundToS(System.currentTimeMillis());
			Set<String> changedAuthenticators = getChangedAuthenticators(roundedUpdateTime);
			Set<String> changedAuthenticationFlows = getChangedAuthenticationFlows(roundedUpdateTime);
			List<Endpoint> endpointsInDBMap = endpointDB.getAll();
			log.debug("There are " + endpointsInDBMap.size() + " endpoints in DB.");
			for (Endpoint endpointInDB: endpointsInDBMap)
			{
				EndpointInstance instance = loader.createEndpointInstance(endpointInDB);
				String name = instance.getEndpointDescription().getName();
				endpointsInDb.add(name);
				EndpointInstance runtimeEndpointInstance = endpointsDeployed.get(name);

				if (runtimeEndpointInstance == null)
				{
					log.info("Endpoint " + name + " will be deployed");
					endpointMan.deploy(instance);
				} else if (endpointInDB.getRevision() > runtimeEndpointInstance.getEndpointDescription().getEndpoint().getRevision())
				{
					log.info("Endpoint " + name + " will be re-deployed");
					endpointMan.undeploy(name);
					endpointMan.deploy(instance);
				} else if (hasChangedAuthenticationFlow(changedAuthenticationFlows, instance))
				{
					updateEndpointAuthenticators(name, instance, endpointsDeployed);
				}else if (hasChangedAuthenticator(changedAuthenticators, instance))
				{
					updateEndpointAuthenticators(name, instance, endpointsDeployed);
				}
			}
			setLastUpdate(roundedUpdateTime);

			undeployRemoved(endpointsInDb, deployedEndpoints);
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
	
	/**
	 * @param sql
	 * @return Set of those authenticators that were updated after the last update of endpoints.
	 * @throws EngineException 
	 */
	private Set<String> getChangedAuthenticators(long roundedUpdateTime) throws EngineException
	{
		Set<String> changedAuthenticators = new HashSet<>();
		List<Map.Entry<String, Date>> authnNames = authnDB.getAllNamesWithUpdateTimestamps();
		for (Map.Entry<String, Date> authn: authnNames)
		{
			long authenticatorChangedAt = roundToS(authn.getValue().getTime());
			log.trace("Authenticator update timestampses: " + roundedUpdateTime + " " + 
					getLastUpdate() + " " + authn.getKey() + ": " + authenticatorChangedAt);
			if (authenticatorChangedAt >= getLastUpdate() && roundedUpdateTime != authenticatorChangedAt)
				changedAuthenticators.add(authn.getKey());
		}
		log.trace("Changed authenticators" + changedAuthenticators);
		return changedAuthenticators;
	}
	
	/**
	 * @param sql
	 * @return Set of those authentication flows that were updated after the last update of endpoints.
	 * @throws EngineException 
	 */
	private Set<String> getChangedAuthenticationFlows(long roundedUpdateTime) throws EngineException
	{
		Set<String> changedAuthenticators = new HashSet<String>();
		List<Map.Entry<String, Date>> authnNames = authnFlowDB.getAllNamesWithUpdateTimestamps();
		for (Map.Entry<String, Date> authn: authnNames)
		{
			long authenticatorChangedAt = roundToS(authn.getValue().getTime());
			log.trace("Authentication flow update timestampses: " + roundedUpdateTime + " " + 
					getLastUpdate() + " " + authn.getKey() + ": " + authenticatorChangedAt);
			if (authenticatorChangedAt >= getLastUpdate() && roundedUpdateTime != authenticatorChangedAt)
				changedAuthenticators.add(authn.getKey());
		}
		log.trace("Changed authentication flows" + changedAuthenticators);
		return changedAuthenticators;
	}
	
	/**
	 * @param changedAuthenticators
	 * @param instance
	 * @return true if endpoint has any of the authenticators in the parameter set
	 */
	private boolean hasChangedAuthenticator(Set<String> changedAuthenticators,
			EndpointInstance instance)
	{
		List<String> authnOptions = instance.getEndpointDescription().getEndpoint()
				.getConfiguration().getAuthenticationOptions();

		
		
		Map<String, AuthenticationFlowDefinition> flows = authnFlowDB.getAllAsMap();
		
		
		for (String authnOption : authnOptions)
		{
			AuthenticationFlowDefinition floDef = flows.get(authnOption);

			if (floDef != null)
				for (String changed : changedAuthenticators)
				{

					if (floDef.getFirstFactorAuthenticators().contains(changed)
							|| floDef.getSecondFactorAuthenticators()
									.contains(changed))
						return true;
				}
			else
			{
				for (String changed : changedAuthenticators)
					if (authnOption.equals(changed))
						return true;
			}
		}
		return false;
	}
	
	/**
	 * @param changedAuthenticationFlow
	 * @param instance
	 * @return true if endpoint has any of the authentication flow in the parameter set
	 */
	private boolean hasChangedAuthenticationFlow(Set<String> changedAuthenticationFlow,
			EndpointInstance instance)
	{
		List<String> authFlows = instance.getEndpointDescription().getEndpoint()
				.getConfiguration().getAuthenticationOptions();

		for (String authnFlowId : authFlows)
		{
			if (changedAuthenticationFlow.contains(authnFlowId))
				return true;
		}
		return false;
	}


	private void undeployRemoved(Set<String> endpointsInDb, Collection<EndpointInstance> deployedEndpoints) 
			throws EngineException
	{
		for (EndpointInstance endpoint: deployedEndpoints)
		{
			String name = endpoint.getEndpointDescription().getName();
			if (!endpointsInDb.contains(name))
			{
				log.info("Undeploying a removed endpoint: " + name);
				endpointMan.undeploy(name);
			}
		}
	}
}
