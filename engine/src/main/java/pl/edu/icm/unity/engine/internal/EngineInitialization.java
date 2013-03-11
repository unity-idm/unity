/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;
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
	private static final Logger log = Log.getLogger(Log.U_SERVER, EngineInitialization.class);
	public static final int ENGINE_INITIALIZATION_MOMENT = 0;
	
	@Autowired
	private InternalEndpointManagement internalEndpointManager;
	@Autowired
	private EndpointManagement endpointManager;
	@Autowired
	private UnityServerConfiguration config;
	@Autowired
	private DBSessionManager db;
	@Autowired
	private DBAttributes dbAttributes;
	@Autowired
	private DBIdentities dbIdentities;
	@Autowired
	private SystemAttributeTypes sysTypes;
	@Autowired
	private IdentityTypesRegistry idTypesReg;
	@Autowired
	private ExecutorsService executors;
	@Autowired
	private EndpointsUpdater updater;
	
	@Override
	public void start()
	{
		initializeDatabaseContents();
		initializeBackgroundTasks();
		super.start();
	}
	
	@Override
	public int getPhase()
	{
		return ENGINE_INITIALIZATION_MOMENT;
	}

	public void initializeBackgroundTasks()
	{
		Runnable endpointsUpdater = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					updater.updateEndpoints();
				} catch (EngineException e)
				{
					log.error("Can't synchronize runtime state of endpoints " +
							"with the persisted endpoints state", e);
				}
			}
		};
		executors.getService().scheduleWithFixedDelay(endpointsUpdater, 120, 60, TimeUnit.SECONDS);
	}
	
	public void initializeDatabaseContents()
	{
		initializeIdentityTypes();
		initializeAttributeTypes();
		initializeEndpoints();
	}
	
	
	private void initializeIdentityTypes()
	{
		log.info("Checking of all identity types are defined");
		Collection<IdentityTypeDefinition> idTypes = idTypesReg.getAll();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<IdentityType> defined = dbIdentities.getIdentityTypes(sql);
			Set<String> existingSet = new HashSet<String>();
			for (IdentityType idType: defined)
				existingSet.add(idType.getIdentityTypeProvider().getId());
			for (IdentityTypeDefinition it: idTypes)
			{
				if (!existingSet.contains(it.getId()))
				{
					log.info("Adding identity type " + it.getId());
					dbIdentities.createIdentityType(sql, it);
				}
					
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	
	private void initializeAttributeTypes()
	{
		log.info("Checking if all system attribute types are defined");
		SqlSession sql = db.getSqlSession(true);
		try
		{
			List<AttributeType> existing = dbAttributes.getAttributeTypes(sql);
			Set<AttributeType> existingSet = new HashSet<AttributeType>();
			existingSet.addAll(existing);
			for (AttributeType at: sysTypes.getSystemAttributes())
			{
				if (!existingSet.contains(at))
				{
					log.info("Adding a system attribute type: " + at.getName());
					dbAttributes.addAttributeType(at, sql);
				}
			}
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}

	}
	
	private void initializeEndpoints()
	{
		if (config.getBooleanValue(UnityServerConfiguration.RECREATE_ENDPOINTS_ON_STARTUP))
		{
			try
			{
				log.info("Removing all persisted endpoints");
				internalEndpointManager.removeAllPersistedEndpoints();
			} catch (EngineException e)
			{
				log.fatal("Can't remove endpoints which are stored in database", e);
				throw new RuntimeEngineException("Can't restore endpoints which are stored in database", e);
			}
		}
		
		try
		{
			log.info("Loading all persisted endpoints");
			internalEndpointManager.loadPersistedEndpoints();
		} catch (EngineException e)
		{
			log.fatal("Can't restore endpoints which are stored in database", e);
			throw new RuntimeEngineException("Can't restore endpoints which are stored in database", e);
		}
		
		//check for cold start - if so, we should load endpoints from configuration
		try
		{
			if (endpointManager.getEndpoints().size() == 0)
			{
				log.info("Loading all configured endpoints");
				loadEndpointsFromConfiguration();
			}
		} catch (Exception e)
		{
			log.fatal("Can't load endpoints which are configured", e);
			throw new RuntimeEngineException("Can't load endpoints which are configured", e);
		}
		

		try
		{
			List<EndpointDescription> endpoints = endpointManager.getEndpoints();
			log.info("Initialized the following endpoints:");
			for (EndpointDescription endpoint: endpoints)
			{
				log.info(" - " + endpoint.getId() + ": " + endpoint.getType().getName() + 
						" " + endpoint.getDescription());
			}
		} catch (Exception e)
		{
			log.fatal("Can't list loaded endpoints", e);
			throw new RuntimeEngineException("Can't list loaded endpoints", e);
		}
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
			String name = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_NAME);
			
			String jsonConfiguration = FileUtils.readFileToString(configFile);
			
			//TODO authn settings
			endpointManager.deploy(type, name, address, description, null, jsonConfiguration);
			log.info(" - " + name + ": " + type + " " + description);
		}
	}
	
}













