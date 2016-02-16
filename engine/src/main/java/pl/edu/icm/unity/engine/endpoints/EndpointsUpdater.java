/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoints;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.generic.authn.AuthenticatorInstanceDB;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.internal.TransactionalRunner;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.utils.ScheduledUpdaterBase;


/**
 * Allows for scanning the DB endpoints state. If it is detected during the scan that runtime configuration 
 * is outdated wrt DB contents, then the reconfiguration is done: existing endpoints are undeployed,
 * and redeployed from configuration.
 * <p>
 * To ensure synchronization this class is used by two components: periodically (to refresh state changes 
 * by another Unity instance) and manually by endpoints management (to refresh the state after local changes 
 * without waiting for the periodic update).
 * <p>
 * Implementation note: this class uses bit complicated logic related to time when the update takes place.
 * This is related to the fact that some DB engines stores the update timestamp with a second precision.
 * This situation is properly handled without 'loosing' updates.
 * @author K. Benedyczak
 */
@Component
public class EndpointsUpdater extends ScheduledUpdaterBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EndpointsUpdater.class);
	private InternalEndpointManagement endpointMan;
	private EndpointDB endpointDB;
	private AuthenticatorInstanceDB authnDB;
	private TransactionalRunner tx;
	
	@Autowired
	public EndpointsUpdater(TransactionalRunner tx,
			InternalEndpointManagement endpointMan, EndpointDB endpointDB,
			AuthenticatorInstanceDB authnDB)
	{
		super("endpoints");
		this.tx = tx;
		this.endpointMan = endpointMan;
		this.endpointDB = endpointDB;
		this.authnDB = authnDB;
	}

	protected void updateInternal() throws EngineException
	{
		List<EndpointInstance> deployedEndpoints = endpointMan.getDeployedEndpoints();
		Set<String> endpointsInDb = new HashSet<String>();
		Map<String, EndpointInstance> endpointsDeployed = new HashMap<>();
		for (EndpointInstance endpoint: deployedEndpoints)
			endpointsDeployed.put(endpoint.getEndpointDescription().getId(), endpoint);
		log.debug("Running periodic endpoints update task. There are " + deployedEndpoints.size() + 
				" deployed endpoints.");
		
		tx.runInTransaction(() -> {
			SqlSession sql = SqlSessionTL.get();
			long roundedUpdateTime = roundToS(System.currentTimeMillis());
			Set<String> changedAuthenticators = getChangedAuthenticators(sql, roundedUpdateTime);

			List<Map.Entry<EndpointInstance, Date>> endpoints = endpointDB.getAllWithUpdateTimestamps(sql);
			log.debug("There are " + endpoints.size() + " endpoints in DB.");
			for (Map.Entry<EndpointInstance, Date> instanceWithDate: endpoints)
			{
				EndpointInstance instance = instanceWithDate.getKey();
				String name = instance.getEndpointDescription().getId();
				endpointsInDb.add(name);
				long endpointLastChange = roundToS(instanceWithDate.getValue().getTime());
				log.trace("Update timestampses: " + roundedUpdateTime + " " + 
						getLastUpdate() + " " + name + ": " + endpointLastChange);
				if (endpointLastChange >= getLastUpdate())
				{
					if (endpointLastChange == roundedUpdateTime)
					{
						log.debug("Skipping update of a changed endpoint to the next round,"
								+ "to prevent doubled update");
						continue;
					}
					if (endpointsDeployed.containsKey(name))
					{
						log.info("Endpoint " + name + " will be re-deployed");
						endpointMan.undeploy(instance.getEndpointDescription().getId());
					} else
						log.info("Endpoint " + name + " will be deployed");

					endpointMan.deploy(instance);
				} else if (hasChangedAuthenticator(changedAuthenticators, instance))
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
			toUpdate.updateAuthenticationOptions(instance.getAuthenticationOptions());
		} catch (UnsupportedOperationException e)
		{
			log.info("Endpoint " + name + " doesn't support authenticators update so will be redeployed");
			endpointMan.undeploy(instance.getEndpointDescription().getId());
			endpointMan.deploy(instance);
		}
	}
	
	/**
	 * @param sql
	 * @return Set of those authenticators that were updated after the last update of endpoints.
	 * @throws EngineException 
	 */
	private Set<String> getChangedAuthenticators(SqlSession sql, long roundedUpdateTime) throws EngineException
	{
		Set<String> changedAuthenticators = new HashSet<String>();
		List<Map.Entry<String, Date>> authnNames = authnDB.getAllNamesWithUpdateTimestamps(sql);
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
	 * @param changedAuthenticators
	 * @param instance
	 * @return true if endpoint has any of the authenticators in the parameter set
	 */
	private boolean hasChangedAuthenticator(Set<String> changedAuthenticators, EndpointInstance instance)
	{
		List<AuthenticationOptionDescription> auths = instance.getEndpointDescription().getAuthenticatorSets();
		for (String changed: changedAuthenticators)
		{
			for (AuthenticationOptionDescription as: auths)
			{
				if (as.contains(changed))
					return true;
			}
		}
		return false;
	}
	
	private void undeployRemoved(Set<String> endpointsInDb, Collection<EndpointInstance> deployedEndpoints) 
			throws EngineException
	{
		for (EndpointInstance endpoint: deployedEndpoints)
		{
			String name = endpoint.getEndpointDescription().getId();
			if (!endpointsInDb.contains(name))
			{
				log.info("Undeploying a removed endpoint: " + name);
				endpointMan.undeploy(endpoint.getEndpointDescription().getId());
			}
		}
	}
}
