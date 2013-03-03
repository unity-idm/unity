/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.utils.LifecycleBase;

/**
 * Responsible for loading the initial state from database and starting background processes.
 * 
 * @author K. Benedyczak
 */
@Component
public class EngineInitialization extends LifecycleBase
{
	private static final Logger log = Logger.getLogger(EngineInitialization.class);
	public static final int ENGINE_INITIALIZATION_MOMENT = 0;
	private EndpointManagementImpl endpointManager;
	private UnityServerConfiguration config;
	
	@Autowired
	public EngineInitialization(EndpointManagementImpl endpointManager,
			UnityServerConfiguration config)
	{
		super();
		this.endpointManager = endpointManager;
		this.config = config;
	}

	@Override
	public void start()
	{
		if (config.getBooleanValue(UnityServerConfiguration.RECREATE_ENDPOINTS_ON_STARTUP))
		{
			log.info("Removing all persisted endpoints");
			endpointManager.removeAllPersistedEndpoints();
		}
		
		try
		{
			log.info("Loading all persisted endpoints");
			endpointManager.loadPersistedEndpoints();
		} catch (EngineException e)
		{
			log.fatal("Can't restore endpoints which are stored in database", e);
			throw new RuntimeEngineException("Can't restore endpoints which are stored in database", e);
		}
		
		//check for cold start - if so, we should load endpoints from configuration
		if (endpointManager.getEndpoints().size() == 0)
		{
			try
			{
				log.info("Loading all configured endpoints");
				loadEndpointsFromConfiguration();
			} catch (Exception e)
			{
				log.fatal("Can't load endpoints which are configured", e);
				throw new RuntimeEngineException("Can't load endpoints which are configured", e);
			}
		}
		
		List<EndpointDescription> endpoints = endpointManager.getEndpoints();
		log.info("Initialized the following endpoints:");
		for (EndpointDescription endpoint: endpoints)
		{
			log.info(" - " + endpoint.getId() + ": " + endpoint.getType().getName() + 
					" " + endpoint.getDescription());
		}
		
		super.start();
	}
	
	@Override
	public int getPhase()
	{
		return ENGINE_INITIALIZATION_MOMENT;
	}

	private void loadEndpointsFromConfiguration() throws IOException, EngineException
	{
		Set<String> endpointsList = config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey: endpointsList)
		{
			String description = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_DESCRIPTION);
			String type = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_TYPE);
			File configFile = config.getFileValue(endpointKey+UnityServerConfiguration.ENDPOINT_CONFIGURATION, false);
			String address = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_ADDRESS);
			
			String jsonConfiguration = FileUtils.readFileToString(configFile);
			
			EndpointDescription endpoint = endpointManager.deploy(type, address, jsonConfiguration);
			//TODO authn settings
			endpointManager.updateEndpoint(endpoint.getId(), description, null, null);
		}
	}
	
}













