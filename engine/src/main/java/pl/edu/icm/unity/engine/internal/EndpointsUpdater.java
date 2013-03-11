/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.engine.AuthenticationManagementImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.endpoint.EndpointInstance;
import pl.edu.icm.unity.server.endpoint.WebAppEndpointInstance;
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
	private long lastUpdate = 0;
	private DBSessionManager db;
	private DBGeneric dbGeneric;
	private JettyServer httpServer;
	private InternalEndpointManagement endpointMan;
	
	@Autowired
	public EndpointsUpdater(DBSessionManager db, DBGeneric dbGeneric,
			JettyServer httpServer, InternalEndpointManagement endpointMan)
	{
		this.db = db;
		this.dbGeneric = dbGeneric;
		this.httpServer = httpServer;
		this.endpointMan = endpointMan;
	}

	public void updateEndpoints() throws EngineException
	{
		synchronized(endpointMan)
		{
			updateEndpointsInt();
		}
	}

	private void updateEndpointsInt() throws EngineException
	{
		List<WebAppEndpointInstance> deployedEndpoints = httpServer.getDeployedEndpoints();
		Set<String> endpointsInDb = new HashSet<String>();
		Set<String> endpointsDeployed = new HashSet<String>();
		for (WebAppEndpointInstance endpoint: deployedEndpoints)
			endpointsDeployed.add(endpoint.getEndpointDescription().getId());
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Set<String> changedAuthenticators = getChangedAuthenticators(sql);
			
			List<GenericObjectBean> endpoints = dbGeneric.getObjectsOfType(
					InternalEndpointManagement.ENDPOINT_OBJECT_TYPE, sql);
			for (GenericObjectBean g: endpoints)
			{
				endpointsInDb.add(g.getName());
				EndpointInstance instance = endpointMan.deserializeEndpoint(
						g.getName(), g.getSubType(), g.getContents(), sql);

				if (g.getLastUpdate().getTime() >= lastUpdate || hasChangedAuthenticator(
						changedAuthenticators, instance))
				{
					if (endpointsDeployed.contains(g.getName()))
						httpServer.undeployEndpoint(g.getName());
					
					httpServer.deployEndpoint((WebAppEndpointInstance) instance);
				}
			}
			lastUpdate = System.currentTimeMillis();

			undeployRemoved(endpointsInDb, deployedEndpoints);
			
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}
	

	private Set<String> getChangedAuthenticators(SqlSession sql)
	{
		Set<String> changedAuthenticators = new HashSet<String>();
		List<GenericObjectBean> authenticators = dbGeneric.getObjectsOfType(
				AuthenticationManagementImpl.AUTHENTICATOR_OBJECT_TYPE, sql);
		for (GenericObjectBean a: authenticators)
		{
			if (a.getLastUpdate().getTime() >= lastUpdate)
				changedAuthenticators.add(a.getName());
		}
		return changedAuthenticators;
	}
	
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
				httpServer.undeployEndpoint(name);
			}
		}
	}
}
