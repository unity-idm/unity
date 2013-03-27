/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordHandlerFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
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
	public static final String DEFAULT_CREDENTIAL = "Password credential";
	public static final String DEFAULT_CREDENTIAL_REQUIREMENT = "Password requirement";
	
	@Autowired
	private InternalEndpointManagement internalEndpointManager;
	@Autowired
	@Qualifier("insecure")
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
	private DBGroups dbGroups;
	@Autowired
	@Qualifier("insecure")
	private IdentitiesManagement idManagement;
	@Autowired
	@Qualifier("insecure")
	private AuthenticationManagement authnManagement;
	@Autowired
	@Qualifier("insecure")
	private AttributesManagement attrManagement;
	@Autowired
	private SystemAttributeTypes sysTypes;
	@Autowired
	private IdentityTypesRegistry idTypesReg;
	@Autowired
	private ExecutorsService executors;
	@Autowired
	private EndpointsUpdater updater;
	@Autowired
	private AttributeStatementsCleaner attributeStatementsCleaner;
	
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

		Runnable attributeStatementsUpdater = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					attributeStatementsCleaner.updateGroups();
				} catch (RuntimeEngineException e)
				{
					log.error("Can't update groups attribute statements", e);
				}
			}
		};
		executors.getService().scheduleWithFixedDelay(attributeStatementsUpdater, 
				120, 600, TimeUnit.SECONDS);
	}
	
	public void initializeDatabaseContents()
	{
		initializeIdentityTypes();
		initializeAttributeTypes();
		initializeAdminUser();
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
	
	private void initializeAdminUser()
	{
		SqlSession sql = db.getSqlSession(true);
		GroupContents contents;
		try
		{
			contents = dbGroups.getContents("/", GroupContents.MEMBERS, sql);
			sql.commit();
		} finally
		{
			db.releaseSqlSession(sql);
		}
		
		try
		{
			if (contents.getMembers().size() == 0)
			{
				log.info("Database contains no users, adding the admin user and the " +
						"default credential settings");
				CredentialDefinition credDef = new CredentialDefinition(PasswordHandlerFactory.ID, 
						DEFAULT_CREDENTIAL, "Default password credential with typical security settings.");
				authnManagement.addCredentialDefinition(credDef);
				
				CredentialRequirements crDef = new CredentialRequirements(DEFAULT_CREDENTIAL_REQUIREMENT, 
						"Default password credential requirement", 
						Collections.singleton(credDef.getName()));
				authnManagement.addCredentialRequirement(crDef);

				IdentityParam admin = new IdentityParam(UsernameIdentity.ID, "admin", true, true);
				Identity adminId = idManagement.addIdentity(admin, crDef.getName(), 
						LocalAuthenticationState.outdated);
				
				EntityParam adminEntity = new EntityParam(adminId.getEntityId());
				idManagement.setEntityCredential(adminEntity, credDef.getName(), "admin");
				EnumAttribute roleAt = new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_LEVEL,
						"/", AttributeVisibility.local, 
						AuthorizationManagerImpl.SYSTEM_MANAGER_ROLE);
				attrManagement.setAttribute(adminEntity, roleAt, false);
			}
		} catch (EngineException e)
		{
			throw new RuntimeEngineException("Initialization problem when creating admin user", e);
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
						" " + endpoint.getDescription() + " at " + 
						endpoint.getContextAddress());
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
			List<AuthenticatorSet> todo = new ArrayList<AuthenticatorSet>();
			endpointManager.deploy(type, name, address, description, todo, jsonConfiguration);
			log.info(" - " + name + ": " + type + " " + description);
		}
	}
	
}













