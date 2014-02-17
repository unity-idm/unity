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
import java.util.Properties;
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
import pl.edu.icm.unity.engine.endpoints.EndpointsUpdater;
import pl.edu.icm.unity.engine.endpoints.InternalEndpointManagement;
import pl.edu.icm.unity.engine.notifications.EmailFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.Message;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.ServerInitializer;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
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
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.IdentityTypeDefinition;
import pl.edu.icm.unity.types.basic.NotificationChannel;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.utils.LifecycleBase;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.FilePropertiesHelper;

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
	@Autowired
	@Qualifier("insecure")
	private NotificationsManagement notManagement;
	@Autowired
	private List<ServerInitializer> initializers;
	
	@Autowired
	private TranslationActionsRegistry tactionsRegistry;
	@Autowired
	private ObjectMapper jsonMapper;
	@Autowired
	@Qualifier("insecure")
	private TranslationProfileManagement profilesManagement;
	@Autowired
	@Qualifier("insecure")
	private MessageTemplateManagement msgTemplatesManagement;
	
	
	
	private long endpointsLoadTime;
	private boolean coldStart = false;
	
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
				} catch (Exception e)
				{
					log.error("Can't synchronize runtime state of endpoints " +
							"with the persisted endpoints state", e);
				}
			}
		};
		int interval = config.getIntValue(UnityServerConfiguration.UPDATE_INTERVAL);
		updater.setLastUpdate(endpointsLoadTime);
		executors.getService().scheduleWithFixedDelay(endpointsUpdater, interval+interval/10, 
				interval, TimeUnit.SECONDS);

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
		//the cleaner is just a cleaner. No need to call it very often.
		executors.getService().scheduleWithFixedDelay(attributeStatementsUpdater, 
				interval*10, interval*10, TimeUnit.SECONDS);
	}
	
	public void initializeDatabaseContents()
	{
		initializeIdentityTypes();
		initializeAttributeTypes();
		initializeAdminUser();
		initializeCredentials();
		initializeCredentialReqirements();
		initializeTranslationProfiles();
		initializeAuthenticators();
		initializeEndpoints();
		initializeNotifications();
		initializeMsgTemplates();
		runInitializers();
	}
	
	
	private void initializeMsgTemplates()
	{
		Map<String, MessageTemplate> existingTemplates;
		try
		{
			existingTemplates = msgTemplatesManagement.listTemplates();
		} catch (EngineException e)
		{
			throw new InternalException("Can't load existing message templates list", e);
		}
		File file = config.getFileValue(UnityServerConfiguration.TEMPLATES_CONF, false);
		
		Properties props = null;
		try
		{
			props = FilePropertiesHelper.load(file);
		} catch (IOException e)
		{
			throw new InternalException("Can't load message templates config file", e);
		}
		
		Set<String> templateKeys = new HashSet<>();
		for (Object keyO: props.keySet())
		{
			String key = (String) keyO;
			if (key.contains("."))
				templateKeys.add(key.substring(0, key.indexOf('.')));
		}	
		
		for(String key:templateKeys)
		{
			if (existingTemplates.keySet().contains(key))
			{
				continue;
			}
			try
			{
					MessageTemplate templ = loadTemplate(props, key);
					msgTemplatesManagement.addTemplate(templ);
			} catch (WrongArgumentException e)
			{
				log.error("Template with id " + key + "not exists", e);
			} catch (EngineException e)
			{
				log.error("Cannot add template " + key, e);
			}
		}
		
	}
	
	private MessageTemplate loadTemplate(Properties properties, String id) throws WrongArgumentException
	{
		String body = properties.getProperty(id+".body");
		String subject = properties.getProperty(id+".subject");
		String consumer = properties.getProperty(id+".consumer", "");
		String description = properties.getProperty(id+".description", "");
		
		if (body == null || subject == null)
			throw new WrongArgumentException("There is no template for this id");
		
		Map<String, Message> msgList = new HashMap<String, Message>();
		Message tempMsg = new Message(subject, body);
		msgList.put("", tempMsg);
//		For future, support to read all locales. 
//		Set<Object> keys = properties.keySet();
//		for (Object keyO: keys)
//		{
//			
//			String key = (String) keyO;
//			String pfx = id + ".body.";
//			String locale;
//			if (key.startsWith(pfx))
//			{       
//				locale = key.substring(pfx.length());
//				if(msgList.containsKey(locale))
//				{
//					msgList.get(locale).setBody(properties.getProperty(key));
//				}else
//				{
//					msgList.put(locale, new Message("", properties.getProperty(key)));
//				}
//				
//			}
//			pfx = id + ".subject.";
//			if (key.startsWith(pfx))
//			{
//				locale = key.substring(pfx.length());
//				if(msgList.containsKey(locale))
//				{
//					msgList.get(locale).setSubject(properties.getProperty(key));
//				}else
//				{
//					msgList.put(locale, new Message(properties.getProperty(key),""));
//				}
//				
//			}
//		
//		}
		return new MessageTemplate(id, description, msgList, consumer);
		
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
					coldStart = true;
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
			Map<String, AttributeType> existing = dbAttributes.getAttributeTypes(sql);
			for (AttributeType at: sysTypes.getSystemAttributes())
			{
				if (!existing.containsKey(at.getName()))
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
		try
		{
			String adminU = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_USER);
			if (adminU == null)
				return;
			String adminP = config.getValue(UnityServerConfiguration.INITIAL_ADMIN_PASSWORD);
			IdentityParam admin = new IdentityParam(UsernameIdentity.ID, adminU, true);
			try
			{
				idManagement.getEntity(new EntityParam(admin));
			} catch (IllegalIdentityValueException e)
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


				
				Identity adminId = idManagement.addEntity(admin, crDef.getName(), EntityState.valid, false);
				
				EntityParam adminEntity = new EntityParam(adminId.getEntityId());
				PasswordToken ptoken = new PasswordToken(adminP);
				idManagement.setEntityCredential(adminEntity, credDef.getName(), ptoken.toJson());
				if (config.getBooleanValue(UnityServerConfiguration.INITIAL_ADMIN_USER_OUTDATED))
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
	
	
	private void initializeNotifications()
	{
		try
		{
			Map<String, NotificationChannel> existingChannels = notManagement.getNotificationChannels();
			for (String key: existingChannels.keySet())
			{
				notManagement.removeNotificationChannel(key);
				log.info("Removed old definition of the notification channel " + key);
			}
			if (!config.isSet(UnityServerConfiguration.MAIL_CONF))
			{
				log.info("Mail configuration file is not set, mail notification channel won't be loaded.");
				return;
			}
			File mailCfgFile = config.getFileValue(UnityServerConfiguration.MAIL_CONF, false);
			String mailCfg = FileUtils.readFileToString(mailCfgFile);
			NotificationChannel emailCh = new NotificationChannel(
					UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL, 
					"Default email channel", mailCfg, EmailFacility.NAME);
			notManagement.addNotificationChannel(emailCh);
			log.info("Created a notification channel: " + emailCh.getName() + " [" + 
					emailCh.getFacilityId() + "]");
		} catch (Exception e)
		{
			log.fatal("Can't load e-mail notification channel configuration", e);
			throw new InternalException("Can't load e-mail notification channel configuration", e);
		}
	}
	
	private void initializeTranslationProfiles()
	{
		List<String> profileFiles = config.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
		log.info("Removing old translation profiles");
		try
		{
			Map<String, TranslationProfile> existingProfiles = profilesManagement.listProfiles();
			for (String pr: existingProfiles.keySet())
				profilesManagement.removeProfile(pr);
		} catch (EngineException e)
		{
			throw new InternalException("Can't wipe existing translation profiles", e);
		}
		log.info("Loading configured translation profiles");
		for (String profileFile: profileFiles)
		{
			String json;
			try
			{
				json = FileUtils.readFileToString(new File(profileFile));
			} catch (IOException e)
			{
				throw new ConfigurationException("Problem loading translation profile from file: " +
						profileFile, e);
			}
			TranslationProfile tp = new TranslationProfile(json, jsonMapper, tactionsRegistry);
			log.info(" - loaded translation profile: " + tp.getName() + " from " + profileFile);
			try
			{
				profilesManagement.addProfile(tp);
			} catch (EngineException e)
			{
				throw new InternalException("Can't install the configured translation profile " 
						+ tp.getName(), e);
			}
		}
	}
	
	private void runInitializers()
	{
		if (!coldStart)
			return;
		List<String> enabledL = config.getListOfValues(UnityServerConfiguration.INITIALIZERS);
		HashSet<String> enabled = new HashSet<>(enabledL);
		for (ServerInitializer initializer: initializers)
		{
			if (enabled.contains(initializer.getName()))
			{
				log.info("Running initializer: " + initializer.getName());
				initializer.run();
			} else
				log.debug("Skipping initializer: " + initializer.getName());
		}
	}

}













