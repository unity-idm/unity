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
	private static final Logger log = Logger.getLogger(EngineInitialization.class);
	public static final int ENGINE_INITIALIZATION_MOMENT = 0;
	private InternalEndpointManagement internalEndpointManager;
	private EndpointManagement endpointManager;
	private UnityServerConfiguration config;
	private DBSessionManager db;
	private DBAttributes dbAttributes;
	private DBIdentities dbIdentities;
	private SystemAttributeTypes sysTypes;
	private IdentityTypesRegistry idTypesReg;
	
	@Autowired
	public EngineInitialization(InternalEndpointManagement internalEndpointManager,
			EndpointManagement endpointManager, UnityServerConfiguration config,
			DBSessionManager db, DBAttributes dbAttributes, DBIdentities dbIdentities,
			SystemAttributeTypes sysTypes, IdentityTypesRegistry idTypesReg)
	{
		this.internalEndpointManager = internalEndpointManager;
		this.endpointManager = endpointManager;
		this.config = config;
		this.db = db;
		this.dbAttributes = dbAttributes;
		this.dbIdentities = dbIdentities;
		this.sysTypes = sysTypes;
		this.idTypesReg = idTypesReg;
	}

	@Override
	public void start()
	{
		initializeIdentityTypes();
		initializeAttributeTypes();
		initializeEndpoints();
		super.start();
	}
	
	@Override
	public int getPhase()
	{
		return ENGINE_INITIALIZATION_MOMENT;
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
			log.info("Removing all persisted endpoints");
			internalEndpointManager.removeAllPersistedEndpoints();
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
			
			EndpointDescription endpoint = endpointManager.deploy(type, name, address, jsonConfiguration);
			//TODO authn settings
			endpointManager.updateEndpoint(endpoint.getId(), description, null, null);
		}
	}
	
}













