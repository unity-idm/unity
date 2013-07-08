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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
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
	
	private long endpointsLoadTime;
	
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
		updater.setLastUpdate(endpointsLoadTime);
		executors.getService().scheduleWithFixedDelay(endpointsUpdater, 120, 60, TimeUnit.SECONDS);

		Runnable attributeStatementsUpdater = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					attributeStatementsCleaner.updateGroups();
				} catch (Exception e)
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
		initializeCredentials();
		initializeCredentialReqirements();
		initializeAuthenticators();
		initializeEndpoints();
	}
	
	
	private void initializeIdentityTypes()
	{
		log.info("Checking if all identity types are defined");
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
		} catch (EngineException e)
		{
			throw new InternalException("Initialization problem when creating attribute types", e);
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
			try
			{
				contents = dbGroups.getContents("/", GroupContents.MEMBERS, sql);
				sql.commit();
			} finally
			{
				db.releaseSqlSession(sql);
			}
			
			if (contents.getMembers().size() == 0)
			{
				log.info("Database contains no users, adding the admin user and the " +
						"default credential settings");
				CredentialDefinition credDef = new CredentialDefinition(PasswordVerificatorFactory.NAME,
						DEFAULT_CREDENTIAL, "Default password credential with typical security settings.");
				credDef.setJsonConfiguration("{\"minLength\": 1," +
						"\"historySize\": 1," +
						"\"minClassesNum\": 1," +
						"\"denySequences\": false," +
						"\"maxAge\": 30758400000}");
				authnManagement.addCredentialDefinition(credDef);
				
				CredentialRequirements crDef = new CredentialRequirements(DEFAULT_CREDENTIAL_REQUIREMENT, 
						"Default password credential requirement", 
						Collections.singleton(credDef.getName()));
				authnManagement.addCredentialRequirement(crDef);

				String adminU = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_USER);
				String adminP = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_PASSWORD);
				IdentityParam admin = new IdentityParam(UsernameIdentity.ID, adminU, true);
				Identity adminId = idManagement.addEntity(admin, crDef.getName(), EntityState.valid, false);
				
				EntityParam adminEntity = new EntityParam(adminId.getEntityId());
				idManagement.setEntityCredential(adminEntity, credDef.getName(), adminP);
				idManagement.setEntityCredentialStatus(adminEntity, credDef.getName(), 
						LocalCredentialState.outdated);
				EnumAttribute roleAt = new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE,
						"/", AttributeVisibility.local, 
						AuthorizationManagerImpl.SYSTEM_MANAGER_ROLE);
				attrManagement.setAttribute(adminEntity, roleAt, false);
				log.warn("IMPORTANT: database was initialized with a default admin user and password." +
						" Log in and change the admin's password immediatelly! U: " + 
						adminU + " P: " + adminP);
			}
		} catch (EngineException e)
		{
			throw new InternalException("Initialization problem when creating admin user", e);
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
				throw new InternalException("Can't restore endpoints which are stored in database", e);
			}
		}
		
		try
		{
			log.info("Loading all persisted endpoints");
			internalEndpointManager.loadPersistedEndpoints();
		} catch (EngineException e)
		{
			log.fatal("Can't restore endpoints which are stored in database", e);
			throw new InternalException("Can't restore endpoints which are stored in database", e);
		}
		
		//check for cold start - if so, we should load endpoints from configuration
		try
		{
			if (endpointManager.getEndpoints().size() == 0)
			{
				loadEndpointsFromConfiguration();
			}
		} catch (Exception e)
		{
			log.fatal("Can't load endpoints which are configured", e);
			throw new InternalException("Can't load endpoints which are configured", e);
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
			throw new InternalException("Can't list loaded endpoints", e);
		}
		endpointsLoadTime = System.currentTimeMillis();
	}
	
	private void loadEndpointsFromConfiguration() throws IOException, EngineException
	{
		log.info("Loading all configured endpoints");
		Set<String> endpointsList = config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey: endpointsList)
		{
			String description = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_DESCRIPTION);
			String type = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_TYPE);
			File configFile = config.getFileValue(endpointKey+UnityServerConfiguration.ENDPOINT_CONFIGURATION, false);
			String address = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_ADDRESS);
			String name = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_NAME);
			String authenticatorsSpec = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_AUTHENTICATORS);
			
			String[] authenticatorSets = authenticatorsSpec.split(";");
			List<AuthenticatorSet> endpointAuthn = new ArrayList<AuthenticatorSet>();
			for (String authenticatorSet: authenticatorSets)
			{
				Set<String> endpointAuthnSet = new HashSet<String>();
				String[] authenticators = authenticatorSet.split(",");
				for (String a: authenticators)
					endpointAuthnSet.add(a.trim());
				endpointAuthn.add(new AuthenticatorSet(endpointAuthnSet));
			}
			
			String jsonConfiguration = FileUtils.readFileToString(configFile);

			endpointManager.deploy(type, name, address, description, endpointAuthn, jsonConfiguration);
			log.info(" - " + name + ": " + type + " " + description);
		}
	}

	private void initializeAuthenticators()
	{
		try
		{
			loadAuthenticatorsFromConfiguration();
		} catch(Exception e)
		{
			log.fatal("Can't load authenticators which are configured", e);
			throw new InternalException("Can't load authenticators which are configured", e);
		}
	}
	
	private void loadAuthenticatorsFromConfiguration() throws IOException, EngineException
	{
		log.info("Loading all configured authenticators");
		Collection<AuthenticatorInstance> authenticators = authnManagement.getAuthenticators(null);
		Map<String, AuthenticatorInstance> existing = new HashMap<String, AuthenticatorInstance>();
		for (AuthenticatorInstance ai: authenticators)
			existing.put(ai.getId(), ai);
		
		Set<String> authenticatorsList = config.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticatorKey: authenticatorsList)
		{
			String name = config.getValue(authenticatorKey+UnityServerConfiguration.AUTHENTICATOR_NAME);
			String type = config.getValue(authenticatorKey+UnityServerConfiguration.AUTHENTICATOR_TYPE);
			File vConfigFile = config.getFileValue(authenticatorKey+
					UnityServerConfiguration.AUTHENTICATOR_VERIFICATOR_CONFIG, false);
			File rConfigFile = config.getFileValue(authenticatorKey+
					UnityServerConfiguration.AUTHENTICATOR_RETRIEVAL_CONFIG, false);
			String credential = config.getValue(authenticatorKey+UnityServerConfiguration.AUTHENTICATOR_CREDENTIAL);

			
			String vJsonConfiguration = vConfigFile == null ? null : FileUtils.readFileToString(vConfigFile);
			String rJsonConfiguration = FileUtils.readFileToString(rConfigFile);
			
			if (!existing.containsKey(name))
			{
				authnManagement.createAuthenticator(name, type, vJsonConfiguration, 
						rJsonConfiguration, credential);
				log.info(" - " + name + " [" + type + "]");
			}
		}
	}

	private void initializeCredentials()
	{
		try
		{
			loadCredentialsFromConfiguration();
		} catch(Exception e)
		{
			log.fatal("Can't load credentials which are configured", e);
			throw new InternalException("Can't load credentials which are configured", e);
		}
	}
	
	private void loadCredentialsFromConfiguration() throws IOException, EngineException
	{
		log.info("Loading all configured credentials");
		Collection<CredentialDefinition> definitions = authnManagement.getCredentialDefinitions();
		Map<String, CredentialDefinition> existing = new HashMap<String, CredentialDefinition>();
		for (CredentialDefinition cd: definitions)
			existing.put(cd.getName(), cd);
		
		Set<String> credentialsList = config.getStructuredListKeys(UnityServerConfiguration.CREDENTIALS);
		for (String credentialKey: credentialsList)
		{
			String name = config.getValue(credentialKey+UnityServerConfiguration.CREDENTIAL_NAME);
			String typeId = config.getValue(credentialKey+UnityServerConfiguration.CREDENTIAL_TYPE);
			String description = config.getValue(credentialKey+UnityServerConfiguration.CREDENTIAL_DESCRIPTION);
			File configFile = config.getFileValue(credentialKey+UnityServerConfiguration.CREDENTIAL_CONFIGURATION, false);

			String jsonConfiguration = FileUtils.readFileToString(configFile);
			CredentialDefinition credentialDefinition = new CredentialDefinition(typeId, name, description);
			credentialDefinition.setJsonConfiguration(jsonConfiguration);
			
			if (!existing.containsKey(name))
			{
				authnManagement.addCredentialDefinition(credentialDefinition);
				log.info(" - " + name + " [" + typeId + "]");
			}
		}
	}

	private void initializeCredentialReqirements()
	{
		try
		{
			loadCredentialRequirementsFromConfiguration();
		} catch(Exception e)
		{
			log.fatal("Can't load configured credential requirements", e);
			throw new InternalException("Can't load configured credential requirements", e);
		}
	}

	private void loadCredentialRequirementsFromConfiguration() throws IOException, EngineException
	{
		log.info("Loading all configured credential requirements");
		Collection<CredentialRequirements> definitions = authnManagement.getCredentialRequirements();
		Map<String, CredentialRequirements> existing = new HashMap<String, CredentialRequirements>();
		for (CredentialRequirements cd: definitions)
			existing.put(cd.getName(), cd);
		
		Set<String> credreqsList = config.getStructuredListKeys(UnityServerConfiguration.CREDENTIAL_REQS);
		for (String credentialKey: credreqsList)
		{
			String name = config.getValue(credentialKey+UnityServerConfiguration.CREDENTIAL_REQ_NAME);
			String description = config.getValue(credentialKey+UnityServerConfiguration.CREDENTIAL_REQ_DESCRIPTION);
			List<String> elements = config.getListOfValues(credentialKey+UnityServerConfiguration.CREDENTIAL_REQ_CONTENTS);
			Set<String> requiredCredentials = new HashSet<String>();
			requiredCredentials.addAll(elements);
			
			CredentialRequirements cr = new CredentialRequirements(name, description, requiredCredentials);
			
			if (!existing.containsKey(name))
			{
				authnManagement.addCredentialRequirement(cr);
				log.info(" - " + name + " " + elements.toString());
			}
		}
	}

}













