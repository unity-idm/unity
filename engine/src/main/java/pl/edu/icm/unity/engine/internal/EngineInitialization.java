/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationServlet;
import pl.edu.icm.unity.db.ContentsUpdater;
import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.db.DBGroups;
import pl.edu.icm.unity.db.DBIdentities;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.InitDB;
import pl.edu.icm.unity.engine.SharedEndpointManagementImpl;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.endpoints.EndpointsUpdater;
import pl.edu.icm.unity.engine.endpoints.InternalEndpointManagement;
import pl.edu.icm.unity.engine.notifications.EmailFacility;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.I18nMessage;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EndpointManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.api.RealmsManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.api.internal.AuthenticatorsManagement;
import pl.edu.icm.unity.server.api.internal.PublicWellKnownURLServlet;
import pl.edu.icm.unity.server.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.server.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.translation.in.InputTranslationProfile;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.FileWatcher;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.ServerInitializer;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.I18nDescribedObject;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
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
	private UnityMessageSource msg;
	@Autowired
	private InternalEndpointManagement internalEndpointManager;
	@Autowired
	@Qualifier("insecure")
	private EndpointManagement endpointManager;
	@Autowired
	private UnityServerConfiguration config;
	@Autowired
	private ContentsUpdater contentsUpdater;
	@Autowired
	private InitDB initDB;
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
	private GroupsManagement groupManagement;
	@Autowired
	@Qualifier("insecure")
	private AuthenticationManagement authnManagement;
	@Autowired
	private AuthenticatorsManagement authenticatorsManagement;
	@Autowired
	@Qualifier("insecure")
	private AttributesManagement attrManagement;
	@Autowired
	private List<SystemAttributesProvider> sysTypeProviders;
	@Autowired
	private IdentityTypesRegistry idTypesReg;
	@Autowired
	private ExecutorsService executors;
	@Autowired
	private EndpointsUpdater updater;
	@Autowired
	EntitiesScheduledUpdater entitiesUpdater;
	@Autowired
	private AttributeStatementsCleaner attributeStatementsCleaner;
	@Autowired
	@Qualifier("insecure")
	private NotificationsManagement notManagement;
	@Autowired
	private List<ServerInitializer> initializers;
	@Autowired
	@Qualifier("insecure")
	private RealmsManagement realmManagement;
	
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
	@Autowired
	private SharedEndpointManagementImpl sharedEndpointManagement;
	@Autowired(required = false)
	private ConfirmationServlet confirmationServlet;
	@Autowired(required = false)
	private PublicWellKnownURLServlet publicWellKnownURLServlet;
	
	
	private long endpointsLoadTime;
	private boolean coldStart = false;
	
	@Override
	public void start()
	{
		updateDatabase();
		
		if (config.getBooleanValue(UnityServerConfiguration.WIPE_DB_AT_STARTUP))
			initDB.resetDatabase();
		
		boolean skipLoading = config.getBooleanValue(
				UnityServerConfiguration.IGNORE_CONFIGURED_CONTENTS_SETTING);
		if (!skipLoading)
			initializeDatabaseContents();
		else
			log.info("Unity is configured to SKIP DATABASE LOADING FROM CONFIGURATION");
		startLogConfigurationMonitoring();
		initializeBackgroundTasks();
		deployConfirmationServlet();
		deployPublicWellKnownURLServlet();
		super.start();
	}
	
	@Override
	public int getPhase()
	{
		return ENGINE_INITIALIZATION_MOMENT;
	}

	private void updateDatabase()
	{
		try
		{
			initDB.updateContents(contentsUpdater);
		} catch (Exception e)
		{
			log.fatal("Update of database contents failded. You have to:\n1) Restore DB from backup\n"
					+ "2) Use the previous version of Unity\n"
					+ "3) Report this problem with the exception following this message to the Unity support mailing list"); 
			throw new InternalException("Update of the database contents failed", e);
		}
	}
	
	private void initializeBackgroundTasks()
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
		updater.setLastUpdate(endpointsLoadTime + 1000); //hack. We set the last update to +1s then the 
		//real value is, to ensure that no immediate endpoint update will take place. This is due to fact that
		//the update precision is stored with 1s granularity. The negative outcome is that any endpoint update
		//in the very first second won't be found. Though chances are minimal (server is still starting...)
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
		
		
		Runnable expiredIdentitiesCleaner = new Runnable()
		{
			@Override
			public void run()
			{
				SqlSession sqlMap = db.getSqlSession(true);
				try
				{
					log.debug("Clearing expired identities");
					dbIdentities.removeExpiredIdentities(sqlMap);
					sqlMap.commit();
				} catch (Exception e)
				{
					log.error("Can't clean expired identities", e);
				} finally
				{
					db.releaseSqlSession(sqlMap);
				}
			}
		};
		executors.getService().scheduleWithFixedDelay(expiredIdentitiesCleaner, 
				interval*100, interval*100, TimeUnit.SECONDS);
		
		
		Runnable entitiesUpdaterTask = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Date nextUpdate = entitiesUpdater.updateEntities();
					executors.getService().schedule(this, 
						nextUpdate.getTime()-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
				} catch (Exception e)
				{
					log.error("Can't perform the scheduled entity operations", e);
				}
			}			
		};
		executors.getService().schedule(entitiesUpdaterTask, (int)(interval*0.5), TimeUnit.SECONDS);
	}
	
	public void initializeDatabaseContents()
	{
		initializeIdentityTypes();
		initializeAttributeTypes();
		initializeAdminUser();
		initializeCredentials();
		initializeCredentialReqirements();
		initializeMsgTemplates();
		initializeNotifications();
		runInitializers();
		initializeTranslationProfiles();
		
		removeERA();
		initializeAuthenticators();
		initializeRealms();
		initializeEndpoints();
	}
	
	private void deployPublicWellKnownURLServlet()
	{
		if (publicWellKnownURLServlet == null)
		{
			log.info("Public well-known URL servlet is not available, skipping its deploymnet");
			return;
		}	
		
		log.info("Deploing public well-known URL servlet");
		ServletHolder holder = new ServletHolder(publicWellKnownURLServlet.getServiceServlet());
		FilterHolder filterHolder = new FilterHolder(publicWellKnownURLServlet.getServiceFilter());
		try
		{
			sharedEndpointManagement.deployInternalEndpointServlet(PublicWellKnownURLServlet.SERVLET_PATH, 
					holder, true);
			sharedEndpointManagement.deployInternalEndpointFilter(PublicWellKnownURLServlet.SERVLET_PATH, 
					filterHolder);
		} catch (EngineException e)
		{
			throw new InternalException("Cannot deploy public well-known URL servlet", e);
		}
	}
		
	private void deployConfirmationServlet()
	{
		if (confirmationServlet == null)
		{
			log.info("Confirmation servlet is not available, skipping its deploymnet");
			return;
		}	
		
		log.info("Deploing confirmation servlet");
		ServletHolder holder = new ServletHolder(confirmationServlet.getServiceServlet());
		FilterHolder filterHolder = new FilterHolder(confirmationServlet.getServiceFilter());
		try
		{
			sharedEndpointManagement.deployInternalEndpointServlet(ConfirmationServlet.SERVLET_PATH, 
					holder, true);
			sharedEndpointManagement.deployInternalEndpointFilter(ConfirmationServlet.SERVLET_PATH, 
					filterHolder);
		} catch (EngineException e)
		{
			throw new InternalException("Cannot deploy internal confirmation servlet", e);
		}
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
		
		I18nMessage tempMsg = new I18nMessage(new I18nString(subject), new I18nString(body));
		return new MessageTemplate(id, description, tempMsg, consumer);
	}
	
	private void initializeIdentityTypes()
	{
		log.info("Checking if all identity types are defined");
		Collection<IdentityTypeDefinition> idTypes = idTypesReg.getAll();
		SqlSession sql = db.getSqlSession(true);
		try
		{
			Map<String, IdentityType> defined = dbIdentities.getIdentityTypes(sql);
			for (IdentityTypeDefinition it: idTypes)
			{
				if (!defined.containsKey(it.getId()))
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
			for (SystemAttributesProvider attrTypesProvider: sysTypeProviders)
				for (AttributeType at: attrTypesProvider.getSystemAttributes())
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
			IdentityParam admin = new IdentityParam(UsernameIdentity.ID, adminU);
			try
			{
				idManagement.getEntity(new EntityParam(admin));
				log.info("There is a user " + adminU + 
						" in the database, admin account will not be created. It is a good idea to remove or comment the "
						+ UnityServerConfiguration.INITIAL_ADMIN_USER + " setting from the main configuration file to "
						+ "disable this message and use it only to add a default user in case of locked access.");
			} catch (IllegalIdentityValueException e)
			{
				log.info("Database contains no admin user, adding the admin user and the " +
						"default credential settings");
				
				CredentialDefinition credDef = createDefaultAdminCredential();
				CredentialRequirements crDef = createDefaultAdminCredReq(credDef.getName());
				
				Identity adminId = createAdminSafe(admin, crDef);
				
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
				log.warn("IMPORTANT:\n"
						+ "Database was initialized with a default admin user and password." +
						" Log in and change the admin's password immediatelly! U: " + 
						adminU + " P: " + adminP + "\n"
						+ "A credential created for this user is named: '" + credDef.getName() + 
						"' make sure that this credential is configured for the admin UI endpoint "
						+ "(if not add a new authenticator definition using thiscredential and add the authenticator to the endpoint)\n"
						+ "A new credential requirement was also created for the new admin user: " + crDef.getName());
			}
		} catch (EngineException e)
		{
			throw new InternalException("Initialization problem when creating admin user", e);
		}
	}
	
	private Identity createAdminSafe(IdentityParam admin, CredentialRequirements crDef) throws EngineException
	{
		try
		{
			return idManagement.addEntity(admin, crDef.getName(), EntityState.valid, false);
		} catch (SchemaConsistencyException e)
		{
			//most probably '/' group attribute class forbids to insert admin. As we need the admin
			//remove ACs and repeat.
			log.warn("There was a schema consistency error adding the admin user. All "
					+ "attribute classes of the '/' group will be removed. Error: " + e.toString());
			GroupContents root = groupManagement.getContents("/", GroupContents.METADATA);
			log.info("Removing ACs: " + root.getGroup().getAttributesClasses());
			root.getGroup().setAttributesClasses(new HashSet<String>());
			groupManagement.updateGroup("/", root.getGroup());
			return idManagement.addEntity(admin, crDef.getName(), EntityState.valid, false);
		}
	}
	
	private CredentialDefinition createDefaultAdminCredential() throws EngineException
	{
		Collection<CredentialDefinition> existingCreds = 
				authnManagement.getCredentialDefinitions();
		String adminCredName = DEFAULT_CREDENTIAL;
		Iterator<CredentialDefinition> credIt = existingCreds.iterator();
		int i=1;
		while (credIt.hasNext())
		{
			CredentialDefinition cred = credIt.next();
			if (cred.getName().equals(adminCredName))
			{
				adminCredName = DEFAULT_CREDENTIAL + "_" + i;
				i++;
				credIt = existingCreds.iterator();
			}
		}
		
		I18nString description = I18nDescribedObject.loadI18nStringFromBundle(
				"CredDef.standardPassword.desc", msg); 
		CredentialDefinition credDef = new CredentialDefinition(PasswordVerificatorFactory.NAME,
				adminCredName, description, msg);
		credDef.setJsonConfiguration("{\"minLength\": 1," +
				"\"historySize\": 1," +
				"\"minClassesNum\": 1," +
				"\"denySequences\": false," +
				"\"maxAge\": 30758400000}");
		authnManagement.addCredentialDefinition(credDef);
		return credDef;
	}
	
	private CredentialRequirements createDefaultAdminCredReq(String credName) throws EngineException
	{
		Collection<CredentialRequirements> existingCRs = 
				authnManagement.getCredentialRequirements();
		String adminCredRName = DEFAULT_CREDENTIAL_REQUIREMENT;
		Iterator<CredentialRequirements> credRIt = existingCRs.iterator();
		int i=1;
		while (credRIt.hasNext())
		{
			CredentialRequirements cr = credRIt.next();
			if (cr.getName().equals(adminCredRName))
			{
				adminCredRName = DEFAULT_CREDENTIAL_REQUIREMENT + "_" + i;
				i++;
				credRIt = existingCRs.iterator();
			}
		}
		
		CredentialRequirements crDef = new CredentialRequirements(adminCredRName, 
				"Default password credential requirement", 
				Collections.singleton(credName));
		authnManagement.addCredentialRequirement(crDef);
		return crDef;
	}
	
	
	/**
	 * Removes all database endpoints, realms and authenticators
	 */
	private void removeERA()
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

		try
		{
			log.info("Removing all persisted realms");
			Collection<AuthenticationRealm> realms = realmManagement.getRealms();
			for (AuthenticationRealm ar: realms)
				realmManagement.removeRealm(ar.getName());
		} catch (EngineException e)
		{
			log.fatal("Can't remove realms which are stored in database", e);
			throw new InternalException("Can't remove realms which are stored in database", e);
		}
		
		try
		{
			log.info("Removing all persisted authenticators");
			authenticatorsManagement.removeAllPersistedAuthenticators();
		} catch (EngineException e)
		{
			log.fatal("Can't remove authenticators which are stored in database", e);
			throw new InternalException("Can't remove authenticators which are stored in database", e);
		}
		
		
	}
	
	private void initializeRealms()
	{
		try
		{
			log.info("Loading configured realms");
			Set<String> realmKeys = config.getStructuredListKeys(UnityServerConfiguration.REALMS);
			for (String realmKey: realmKeys)
			{
				String name = config.getValue(realmKey+UnityServerConfiguration.REALM_NAME);
				String description = config.getValue(realmKey+
						UnityServerConfiguration.REALM_DESCRIPTION);
				int blockAfter = config.getIntValue(realmKey+
						UnityServerConfiguration.REALM_BLOCK_AFTER_UNSUCCESSFUL);
				int blockFor = config.getIntValue(realmKey+UnityServerConfiguration.REALM_BLOCK_FOR);
				int remeberMe = config.getIntValue(realmKey+
						UnityServerConfiguration.REALM_REMEMBER_ME);
				int maxInactive = config.getIntValue(realmKey+
						UnityServerConfiguration.REALM_MAX_INACTIVITY);
				
				realmManagement.addRealm(new AuthenticationRealm(name, description, blockAfter, 
						blockFor, remeberMe, maxInactive));
				description = description == null ? "" : description;
				log.info(" - " + name + ": " + description + " [blockAfter " + 
						blockAfter + ", blockFor " + blockFor + 
						", rememberMe " + remeberMe + ", maxInactive " + maxInactive);
			}
		} catch (EngineException e)
		{
			log.fatal("Can't add realms which are defined in configuration", e);
			throw new InternalException("Can't add realms which are defined in configuration", e);
		}
	}
	
	private void initializeEndpoints()
	{
		try
		{
			loadEndpointsFromConfiguration();
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
						endpoint.getContextAddress() + " in realm " + 
						endpoint.getRealm().getName());
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
			I18nString displayedName = config.getLocalizedString(msg, 
					endpointKey+UnityServerConfiguration.ENDPOINT_DISPLAYED_NAME);
			if (displayedName.isEmpty())
				displayedName.setDefaultValue(name);
			String realmName = config.getValue(endpointKey+UnityServerConfiguration.ENDPOINT_REALM);
			
			List<AuthenticationOptionDescription> endpointAuthn = config.getEndpointAuth(endpointKey);
			String jsonConfiguration = FileUtils.readFileToString(configFile);

			log.info(" - " + name + ": " + type + " " + description);
			endpointManager.deploy(type, name, displayedName, address, description, endpointAuthn, 
					jsonConfiguration, realmName);
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

			
			String vJsonConfiguration = vConfigFile == null ? null : FileUtils.readFileToString(vConfigFile,
					StandardCharsets.UTF_8);
			String rJsonConfiguration = rConfigFile == null ? null : FileUtils.readFileToString(rConfigFile,
					StandardCharsets.UTF_8);
			
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
			CredentialDefinition credentialDefinition = new CredentialDefinition(typeId, name, 
					new I18nString(name), 
					new I18nString(description));
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
		Map<String, InputTranslationProfile> existingProfiles;
		try
		{
			existingProfiles = profilesManagement.listInputProfiles();
		} catch (EngineException e1)
		{
			throw new InternalException("Can't list the existing translation profiles", e1);
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
			InputTranslationProfile tp = new InputTranslationProfile(json, jsonMapper, tactionsRegistry);
			try
			{
				if (existingProfiles.containsKey(tp.getName()))
				{
					log.info(" - updated the in-DB translation profile : " + tp.getName() + 
							" with file definition: " + profileFile);
					profilesManagement.updateProfile(tp);	
				} else
				{
					profilesManagement.addProfile(tp);
					log.info(" - loaded translation profile: " + tp.getName() + 
							" from file: " + profileFile);
				}
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

	private void startLogConfigurationMonitoring()
	{
		final String logConfig = System.getProperty("log4j.configuration");
		if (logConfig == null)
		{
			log.warn("No log configuration file set.");
			return;
		}
		Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					reConfigureLog4j(logConfig);
				} catch (MalformedURLException me)
				{
					throw new RuntimeException(me);
				}
			}
		};
		try
		{
			File logProperties = logConfig.startsWith("file:") ? new File(new URI(logConfig))
					: new File(logConfig);
			FileWatcher fw = new FileWatcher(logProperties, r);
			final int DELAY = 7;
			executors.getService().scheduleWithFixedDelay(fw, DELAY, DELAY, TimeUnit.SECONDS);
			log.info("Started logging subsystem configuration file monitoring with " + 
					DELAY + "s interval.");
		} catch (URISyntaxException e)
		{
			log.warn("Logging configuration file is not a valid URI: '"+logConfig+"'", e);
		} catch (FileNotFoundException e)
		{
			log.warn("Logging configuration file '"+logConfig+"' not found.");
		}
	}

	/**
	 * Re-configure log4j from the named properties file.
	 */
	private void reConfigureLog4j(String logConfig) throws MalformedURLException
	{
		log.info("Logging subsystem configuration file was modified, re-configuring logging.");
		if (logConfig.startsWith("file:"))
		{
			PropertyConfigurator.configure(new URL(logConfig));
		} else
		{
			PropertyConfigurator.configure(logConfig);
		}
	}
}







