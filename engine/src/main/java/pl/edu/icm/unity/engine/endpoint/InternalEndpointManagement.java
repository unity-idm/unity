/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.endpoint;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.store.api.generic.EndpointDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.Endpoint.EndpointState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the internal endpoint management. 
 * This is used internally and not exposed by the public interfaces.
 * <p>
 * NOTE: make sure to fix transactions if this class is refactored to implement an interface! 
 * @author K. Benedyczak
 */
@Component
public class InternalEndpointManagement
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, InternalEndpointManagement.class);
	private EndpointDB endpointDB;
	
	private Map<String, EndpointInstance> deployedEndpoints = new LinkedHashMap<>();
	private EndpointInstanceLoader loader;
	
	@Autowired
	public InternalEndpointManagement(EndpointDB endpointDB, EndpointInstanceLoader loader)
	{
		this.endpointDB = endpointDB;
		this.loader = loader;
	}

	/**
	 * Queries DB for persisted endpoints and loads them.
	 * Should be run only on startup
	 * @throws EngineException 
	 */
	@Transactional
	public synchronized void loadPersistedEndpoints() throws EngineException
	{
		List<Endpoint> fromDb = endpointDB.getAll();
		for (Endpoint endpoint: fromDb)
		{
			if (endpoint.getState().equals(EndpointState.UNDEPLOYED))
				continue;
			
			try
			{
				EndpointInstance instance = loader.createEndpointInstance(endpoint);
				deploy(instance);
				log.info(" - " + endpoint.getName() + ": " + endpoint.getTypeId() + 
					" " + endpoint.getConfiguration().getDescription());
			}
			catch (Exception e)
			{
				if(e instanceof IllegalEndpointException)
					throw e;
				log.error("Can't load endpoint " + endpoint.getName() +
						" of type " + endpoint.getTypeId(), e);
			}
		}
	}	

	@Transactional
	public synchronized void removeAllPersistedEndpoints() throws EngineException
	{
		endpointDB.deleteAll();
		undeployAll();
	}
	
	public synchronized void deploy(EndpointInstance instance) throws EngineException
	{
		instance.start();
		deployedEndpoints.put(instance.getEndpointDescription().getName(), instance);
	}

	public synchronized void undeploy(String instanceId) throws EngineException
	{
		EndpointInstance instance = deployedEndpoints.get(instanceId);
		if (instance == null)
			return;
		instance.destroy();
		deployedEndpoints.remove(instanceId);
	}

	public synchronized List<EndpointInstance> getDeployedEndpoints()
	{
		return new ArrayList<>(deployedEndpoints.values());
	}
	
	public synchronized void undeployAll() throws EngineException
	{
		List<String> keys = new ArrayList<>(deployedEndpoints.keySet());
		for (String endpointId: keys)
			undeploy(endpointId);
	}
	
}
