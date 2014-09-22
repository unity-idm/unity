/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoints;

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

import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.authn.AuthenticatorInstanceDB;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;


/**
 * Allows for scanning the DB endpoints state. If it is detected during the scan that runtime configuration 
 * is outdated wrt DB contents, then the reconfiguration is done: existing endpoints are undeployed,
 * and redeployed from configuration.
 * @author K. Benedyczak
 */
@Component
public class EndpointsUpdater
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EndpointsUpdater.class);
	private long lastUpdate = 0;
	private DBSessionManager db;
	private JettyServer httpServer;
	private InternalEndpointManagement endpointMan;
	private EndpointDB endpointDB;
	private AuthenticatorInstanceDB authnDB;
	
	@Autowired
	public EndpointsUpdater(DBSessionManager db, JettyServer httpServer,
			InternalEndpointManagement endpointMan, EndpointDB endpointDB,
			AuthenticatorInstanceDB authnDB)
	{
		this.db = db;
		this.httpServer = httpServer;
		this.endpointMan = endpointMan;
		this.endpointDB = endpointDB;
		this.authnDB = authnDB;
	}

	public void updateEndpoints() throws EngineException
	{
		synchronized(endpointMan)
		{
			updateEndpointsInt();
		}
	}

	public void setLastUpdate(long lastUpdate)
	{
		this.lastUpdate = (lastUpdate/1000)*1000;
	}

	private void updateEndpointsInt() throws EngineException
	{
		List<WebAppEndpointInstance> deployedEndpoints = httpServer.getDeployedEndpoints();
		Set<String> endpointsInDb = new HashSet<String>();
		Map<String, WebAppEndpointInstance> endpointsDeployed = new HashMap<String, WebAppEndpointInstance>();
		for (WebAppEndpointInstance endpoint: deployedEndpoints)
			endpointsDeployed.put(endpoint.getEndpointDescription().getId(), endpoint);
		log.debug("Running periodic endpoints update task. There are " + deployedEndpoints.size() + 
				" deployed endpoints.");
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Set<String> changedAuthenticators = getChangedAuthenticators(sql);

			List<Map.Entry<EndpointInstance, Date>> endpoints = endpointDB.getAllWithUpdateTimestamps(sql);
			for (Map.Entry<EndpointInstance, Date> instanceWithDate: endpoints)
			{
				EndpointInstance instance = instanceWithDate.getKey();
				String name = instance.getEndpointDescription().getId();
				endpointsInDb.add(name);
				if (instanceWithDate.getValue().getTime() >= lastUpdate)
				{
					if (endpointsDeployed.containsKey(name))
					{
						log.info("Endpoint " + name + " will be re-deployed");
						httpServer.undeployEndpoint(name);
					} else
						log.info("Endpoint " + name + " will be deployed");
					
					httpServer.deployEndpoint((WebAppEndpointInstance) instance);
				} else if (hasChangedAuthenticator(changedAuthenticators, instance))
				{
					updateEndpointAuthenticators(name, instance, endpointsDeployed);
				}
			}
			setLastUpdate(System.currentTimeMillis());

			undeployRemoved(endpointsInDb, deployedEndpoints);
			
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	
	private void updateEndpointAuthenticators(String name, EndpointInstance instance,
			Map<String, WebAppEndpointInstance> endpointsDeployed) throws EngineException
	{
		log.info("Endpoint " + name + " will have its authenticators updated");
		WebAppEndpointInstance toUpdate = endpointsDeployed.get(name);
		try
		{
			toUpdate.updateAuthenticators(instance.getAuthenticators());
		} catch (UnsupportedOperationException e)
		{
			log.info("Endpoint " + name + " doesn't support authenticators update so will be redeployed");
			httpServer.undeployEndpoint(name);
			httpServer.deployEndpoint((WebAppEndpointInstance) instance);
		}
	}
	
	/**
	 * @param sql
	 * @return Set of those authenticators that were updated after the last update of endpoints.
	 * @throws EngineException 
	 */
	private Set<String> getChangedAuthenticators(SqlSession sql) throws EngineException
	{
		Set<String> changedAuthenticators = new HashSet<String>();
		List<Map.Entry<String, Date>> authnNames = authnDB.getAllNamesWithUpdateTimestamps(sql);
		for (Map.Entry<String, Date> authn: authnNames)
		{
			if (authn.getValue().getTime() >= lastUpdate)
				changedAuthenticators.add(authn.getKey());
		}
		return changedAuthenticators;
	}
	
	/**
	 * @param changedAuthenticators
	 * @param instance
	 * @return true if endpoint has any of the authenticators in the parameter set
	 */
	private boolean hasChangedAuthenticator(Set<String> changedAuthenticators, EndpointInstance instance)
	{
		List<AuthenticatorSet> auths = instance.getEndpointDescription().getAuthenticatorSets();
		for (AuthenticatorSet as: auths)
		{
			Set<String> authenticators = as.getAuthenticators();
			for (String a: authenticators)
				if (changedAuthenticators.contains(a))
					return true;
		}
		return false;
	}
	
	private void undeployRemoved(Set<String> endpointsInDb, List<WebAppEndpointInstance> deployedEndpoints) 
			throws EngineException
	{
		for (WebAppEndpointInstance endpoint: deployedEndpoints)
		{
			String name = endpoint.getEndpointDescription().getId();
			if (!endpointsInDb.contains(name))
			{
				log.info("Undeploying a removed endpoint: " + name);
				httpServer.undeployEndpoint(name);
			}
		}
	}
}
