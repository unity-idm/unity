/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.CONFIG_ONLY_ERA_CONTROL;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.USE_CONFIG_FILE_AS_INITIAL_TEMPLATE_ONLY;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.ee8.servlet.FilterHolder;
import org.eclipse.jetty.ee8.servlet.ServletHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.CredentialDefinition;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.base.event.PersistableEvent;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.Identity;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.identity.IdentityType;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ProfileMode;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.CredentialRequirementManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.attributes.SystemAttributesProvider;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInfo;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationServletProvider;
import pl.edu.icm.unity.engine.api.endpoint.ServletProvider;
import pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement;
import pl.edu.icm.unity.engine.api.event.EventCategory;
import pl.edu.icm.unity.engine.api.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;
import pl.edu.icm.unity.engine.api.server.ServerInitializer;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.engine.api.wellknown.AttributesContentPublicServletProvider;
import pl.edu.icm.unity.engine.api.wellknown.PublicWellKnownURLServletProvider;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.audit.AuditEventListener;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.bulkops.BulkOperationsUpdater;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.engine.credential.EntityCredentialsHelper;
import pl.edu.icm.unity.engine.credential.SystemAllCredentialRequirements;
import pl.edu.icm.unity.engine.endpoint.EndpointsUpdater;
import pl.edu.icm.unity.engine.endpoint.InternalEndpointManagement;
import pl.edu.icm.unity.engine.events.EventProcessor;
import pl.edu.icm.unity.engine.group.AttributeStatementsCleaner;
import pl.edu.icm.unity.engine.identity.EntitiesScheduledUpdater;
import pl.edu.icm.unity.engine.identity.IdentityCleaner;
import pl.edu.icm.unity.engine.msg.MessageRepository;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateInitializatior;
import pl.edu.icm.unity.engine.scripts.ScriptTriggeringEventListener;
import pl.edu.icm.unity.engine.translation.TranslationProfileChecker;
import pl.edu.icm.unity.engine.translation.in.SystemInputTranslationProfileProvider;
import pl.edu.icm.unity.engine.translation.out.SystemOutputTranslationProfileProvider;
import pl.edu.icm.unity.engine.utils.LifecycleBase;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.IdentityTypeDAO;
import pl.edu.icm.unity.store.api.generic.AuthenticationFlowDB;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;

/**
 * Responsible for loading the initial state from database and starting
 * background processes.
 * 
 * FIXME - this class needs refactoring: should be split into several classes
 * 
 * @author K. Benedyczak
 */
@Component
public class EngineInitialization extends LifecycleBase
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, UnityServerConfiguration.class);
	public static final int ENGINE_INITIALIZATION_MOMENT = 0;
	public static final String DEFAULT_CREDENTIAL = CredentialManagement.DEFAULT_CREDENTIAL;
	public static final String DEFAULT_CREDENTIAL_REQUIREMENT = SystemAllCredentialRequirements.NAME;


	@Autowired
	private MessageSource msg;
	@Autowired
	private InternalEndpointManagement internalEndpointManager;
	@Autowired
	@Qualifier("insecure")
	private EndpointManagement endpointManager;
	@Autowired
	private UnityServerConfiguration config;
	@Autowired
	private TransactionalRunner tx;
	@Autowired
	private AttributeTypeDAO attributeTypeDAO;
	@Autowired
	private AttributeTypeHelper atHelper;
	@Autowired
	private IdentityTypeDAO dbIdentities;
	@Autowired
	@Qualifier("insecure")
	private EntityManagement idManagement;
	@Autowired
	@Qualifier("insecure")
	private EntityCredentialManagement idCredManagement;
	@Autowired
	@Qualifier("insecure")
	private GroupsManagement groupManagement;
	@Autowired
	@Qualifier("insecure")
	private CredentialRequirementManagement credReqMan;
	@Autowired
	@Qualifier("insecure")
	private CredentialManagement credMan;
	@Autowired
	private IdentityCleaner identityCleaner;
	@Autowired
	@Qualifier("insecure")
	private AuthenticatorManagement authnManagement;
	@Autowired
	private AuthenticatorConfigurationDB authenticatorDAO;
	@Autowired
	private AuthenticationFlowDB authenticationFlowDAO;
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
	private EndpointsUpdater endpointsUpdater;
	@Autowired
	private BulkOperationsUpdater bulkOperationsUpdater;
	@Autowired
	private EntitiesScheduledUpdater entitiesUpdater;
	@Autowired
	private AttributeStatementsCleaner attributeStatementsCleaner;
	@Autowired
	private NotificationChannelsLoader notificationChannelLoader;
	@Autowired
	private MessageTemplateInitializatior msgTemplateLoader;
	@Autowired
	private Optional<List<ServerInitializer>> initializers;
	@Autowired
	@Qualifier("insecure")
	private RealmsManagement realmManagement;
	@Autowired
	@Qualifier("insecure")
	private TranslationProfileManagement profilesManagement;
	@Autowired
	private SharedEndpointManagement sharedEndpointManagement;
	@Autowired(required = false)
	private EmailConfirmationServletProvider confirmationServletFactory;
	@Autowired
	private EventProcessor eventsProcessor;
	@Autowired
	private ScriptTriggeringEventListener scriptEventsConsumer;
	@Autowired
	private AuditEventListener auditEventListener;
	@Autowired(required = false)
	private PublicWellKnownURLServletProvider publicWellKnownURLServlet;
	@Autowired
	TranslationProfileChecker profileHelper;
	@Autowired
	private SystemInputTranslationProfileProvider systemInputProfileProvider;
	@Autowired
	private SystemOutputTranslationProfileProvider systemOutputProfileProvider;
	@Autowired
	private CredentialRepository credRepo;
	@Autowired
	private EntityCredentialsHelper entityCredHelper;
	@Autowired
	@Qualifier("insecure")
	private AuthenticationFlowManagement authnFlowManagement;
	@Autowired(required = false)
	private AttributesContentPublicServletProvider attributesContentServletFactory;
	@Autowired
	@Qualifier("insecure")
	private PKIManagement pkiManagement;
	@Autowired
	private MessageRepository messageRepository;
	
	private long endpointsLoadTime;

	@Override
	public void start()
	{
		try
		{
			initializeMessageRepository();
			installEventListeners();
			endpointsLoadTime = System.currentTimeMillis();
			boolean skipLoading = config
				.getBooleanValue(UnityServerConfiguration.IGNORE_CONFIGURED_CONTENTS_SETTING);
			if (!skipLoading)
				initializeDatabaseContents();
			else
				log.info("Unity is configured to SKIP DATABASE LOADING FROM CONFIGURATION");
			initializeBackgroundTasks();
			deployAttributeContentPublicServlet();
			super.start();
		} catch (Exception e)
		{
			log.error("Fatal error initializating server.", e);
			throw e;
		}
	}

	private void initializeMessageRepository()
	{
		tx.runInTransaction(() -> messageRepository.reload());
	}

	@Override
	public int getPhase()
	{
		return ENGINE_INITIALIZATION_MOMENT;
	}

	private void initializeBackgroundTasks()
	{
		int interval = config.getIntValue(UnityServerConfiguration.UPDATE_INTERVAL);
		endpointsUpdater.setInitialUpdate(endpointsLoadTime);
		executors.getScheduledService().scheduleWithFixedDelay(endpointsUpdater, interval + interval / 10, interval,
				TimeUnit.SECONDS);

		executors.getScheduledService().scheduleWithFixedDelay(bulkOperationsUpdater, interval + 10, interval,
				TimeUnit.SECONDS);

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
		// the cleaner is just a cleaner. No need to call it very often.
		executors.getScheduledService().scheduleWithFixedDelay(attributeStatementsUpdater, interval * 10, interval * 10,
				TimeUnit.SECONDS);

		Runnable expiredIdentitiesCleaner = new Runnable()
		{
			@Override
			public void run()
			{
				log.debug("Clearing expired identities");
				try
				{
					tx.runInTransaction(() -> {
						identityCleaner.removeExpiredIdentities();
					});
				} catch (Exception e)
				{
					log.error("Can't clean expired identities", e);
				}
			}
		};
		executors.getScheduledService().scheduleWithFixedDelay(expiredIdentitiesCleaner, interval * 100, interval * 100,
				TimeUnit.SECONDS);

		Runnable entitiesUpdaterTask = new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Date nextUpdate = entitiesUpdater.updateEntities();
					executors.getScheduledService().schedule(this,
							nextUpdate.getTime() - System.currentTimeMillis(),
							TimeUnit.MILLISECONDS);
				} catch (Exception e)
				{
					log.error("Can't perform the scheduled entity operations", e);
				}
			}
		};
		executors.getScheduledService().schedule(entitiesUpdaterTask, (int) (interval * 0.5), TimeUnit.SECONDS);

		// wait to ensure that we return only when endpoint updates will
		// be caught
		try
		{
			Thread.sleep(1000 - (System.currentTimeMillis() - endpointsLoadTime));
		} catch (InterruptedException e)
		{
			// ok
		}

	}

	public void initializeDatabaseContents()
	{
		boolean isColdStart = determineIfColdStart();
		boolean loadElementsConfiguredInFile = isColdStart || !config.getBooleanValue(USE_CONFIG_FILE_AS_INITIAL_TEMPLATE_ONLY);
		
		if (loadElementsConfiguredInFile)
			initializeSystemContentsFromConfigFile(isColdStart);
		else
			initializeSystemContentsFromDBOnly();

		eventsProcessor.fireEvent(new PersistableEvent(EventCategory.POST_INIT, Boolean.toString(isColdStart)));
	}

	private void initializeSystemContentsFromConfigFile(boolean isColdStart)
	{
		initializeIdentityTypes();
		initializeSystemAttributeTypes();
		
		initializeAdminUser();
		
		initializeCredentials();
		initializeCredentialReqirements();

		notificationChannelLoader.initialize();

		msgTemplateLoader.initializeMsgTemplates();
		
		pkiManagement.loadCertificatesFromConfigFile();

		runInitializers();

		eventsProcessor.fireEvent(new PersistableEvent(EventCategory.PRE_INIT, Boolean.toString(isColdStart)));

		boolean updateExisting = config.getBooleanValue(CONFIG_ONLY_ERA_CONTROL);
		initializeTranslationProfiles(updateExisting);
		checkSystemTranslationProfiles();
		if (updateExisting)
			removeERA();
		initializeAuthenticators();
		initializeAuthenticationFlows();
		initializeRealms();
		initializeEndpoints();
	}

	private void initializeSystemContentsFromDBOnly()
	{
		boolean isColdStart = false;
		loadCertificatesFromFileAfterMigration();
		initializeIdentityTypes();
		initializeSystemAttributeTypes();
		initializeAdminUser();
		notificationChannelLoader.initialize();
		runInitializers();

		eventsProcessor.fireEvent(new PersistableEvent(EventCategory.PRE_INIT, Boolean.toString(isColdStart)));
		deployPersistedEndpoints();
	}

	private void loadCertificatesFromFileAfterMigration()
	{
		try
		{
			//this condition is incorrect - we should check what was the DB version at server start, before migration was run.
			//with this if, user who deletes all certificates will get certs from file after restart.
			if (pkiManagement.getAllCertificateNames().isEmpty())
			{
				log.info("Loading certificates configured in files despite " 
						+ USE_CONFIG_FILE_AS_INITIAL_TEMPLATE_ONLY 
						+ " as no certificates are present");
				pkiManagement.loadCertificatesFromConfigFile();
			}
		} catch (EngineException e)
		{
			throw new InternalException("Initialization problem: can't populate DB with trusted certificates", e);
		}		
	}
	
	
	private void deployPersistedEndpoints()
	{
		try
		{
			internalEndpointManager.loadPersistedEndpoints();
		} catch (EngineException e)
		{
			throw new InternalException("Initialization problem: can't deploy endpoints stored in DB", e);
		}
		logEndpoints();
	}

	private boolean determineIfColdStart()
	{
		try
		{
			List<IdentityType> idTypes = tx.runInTransactionRet(() -> {
				return dbIdentities.getAll();
			});
			return idTypes.isEmpty();
		} catch (Exception e)
		{
			throw new InternalException("Initialization problem when checking identity types.", e);
		}
	}

	private void installEventListeners()
	{
		eventsProcessor.addEventListener(scriptEventsConsumer);
		eventsProcessor.addEventListener(auditEventListener);
	}
	
	private void deployAttributeContentPublicServlet()
	{
		deploySharedEndpointServletWithoutVaadinSupport(attributesContentServletFactory, 
				AttributesContentPublicServletProvider.SERVLET_PATH,
				"public attribute exposure");
	}

	private void deploySharedEndpointServletWithoutVaadinSupport(ServletProvider servletProvider, String path, String name)
	{
		deploySharedEndpointServlet(servletProvider, path, name);
	}
	
	private void deploySharedEndpointServlet(ServletProvider servletProvider, String path, String name)
	{
		if (servletProvider == null)
		{
			log.info("{} servlet factory is not available, skipping its deploymnet", name);
			return;
		}

		log.info("Deploing {} servlet", name);
		ServletHolder holder = servletProvider.getServiceServlet();
		List<FilterHolder> filterHolders = servletProvider.getServiceFilters();
		try
		{
			sharedEndpointManagement.deployInternalEndpointServlet(path, holder, false);
			for (FilterHolder filter: filterHolders)
				sharedEndpointManagement.deployInternalEndpointFilter(path, filter);
		} catch (EngineException e)
		{
			throw new InternalException("Can not deploy " + name + " servlet", e);
		}
	}
	

	private void initializeIdentityTypes()
	{
		log.info("Checking if all identity types are defined");
		Collection<IdentityTypeDefinition> idTypes = idTypesReg.getAll();
		tx.runInTransaction(() -> {
			Map<String, IdentityType> defined = dbIdentities.getAllAsMap();
			for (IdentityTypeDefinition it : idTypes)
			{
				if (!defined.containsKey(it.getId()))
				{
					log.info("Adding identity type " + it.getId());
					IdentityType idType = new IdentityType(it.getId(), it.getId());
					idType.setDescription(msg.getMessage(it.getDefaultDescriptionKey()));
					dbIdentities.create(idType);
				}
			}
		});
	}

	private void initializeSystemAttributeTypes()
	{
		log.info("Checking if all system attribute types are defined");
		tx.runInTransaction(() -> {
			Map<String, AttributeType> existing = attributeTypeDAO.getAllAsMap();
			for (SystemAttributesProvider attrTypesProvider : sysTypeProviders)
				for (AttributeType at : attrTypesProvider.getSystemAttributes())
				{
					AttributeType existingAt = existing.get(at.getName());
					if (existingAt == null)
					{
						log.info("Adding a system attribute type: " + at.getName());
						atHelper.setDefaultSyntaxConfiguration(at);
						attributeTypeDAO.create(at);
					} else if (attrTypesProvider.requiresUpdate(existingAt))
					{
						log.info("Updating a system attribute type: " + at.getName());
						attributeTypeDAO.update(at);
					}
				}
		});
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
				log.info("There is a user " + adminU
						+ " in the database, admin account will not be created. It is a good idea to remove or comment the "
						+ UnityServerConfiguration.INITIAL_ADMIN_USER
						+ " setting from the main configuration file to "
						+ "disable this message and use it only to add a default user in case of locked access.");
			} catch (UnknownIdentityException e)
			{
				log.info("Database contains no admin user, creating the configured admin user");
				CredentialDefinition credDef = credRepo.get(DEFAULT_CREDENTIAL);
				Identity adminId = createAdminSafe(admin, SystemAllCredentialRequirements.NAME);

				EntityParam adminEntity = new EntityParam(adminId.getEntityId());
				PasswordToken ptoken = new PasswordToken(adminP);

				// Set password without verification
				tx.runInTransactionThrowing(() -> {
					entityCredHelper.setEntityCredentialInternalWithoutVerify(
							adminEntity.getEntityId(), credDef.getName(), ptoken.toJson());
				});

				if (config.getBooleanValue(UnityServerConfiguration.INITIAL_ADMIN_USER_OUTDATED))
					idCredManagement.setEntityCredentialStatus(adminEntity, credDef.getName(),
							LocalCredentialState.outdated);

				Attribute roleAt = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "/",
						Lists.newArrayList(InternalAuthorizationManagerImpl.SYSTEM_MANAGER_ROLE));
				attrManagement.createAttribute(adminEntity, roleAt);
				log.warn("IMPORTANT:\n"
						+ "Database was initialized with a default admin user and password."
						+ " Log in and change the admin's password immediatelly! U: " + adminU
						+ " P: " + adminP + "\n"
						+ "The credential used for this user is named: '" + credDef.getName()
						+ "' make sure that this credential is enabled for the admin UI endpoint. "
						+ "If not add an authenticator using this credential to the admin endpoint.");
			}
		} catch (EngineException e)
		{
			throw new InternalException("Initialization problem when creating admin user", e);
		}
	}

	private Identity createAdminSafe(IdentityParam admin, String crDef) throws EngineException
	{
		try
		{
			return idManagement.addEntity(admin, crDef, EntityState.valid);
		} catch (SchemaConsistencyException e)
		{
			// most probably '/' group attribute class forbids to
			// insert admin. As we need the admin
			// remove ACs and repeat.
			log.warn("There was a schema consistency error adding the admin user. All "
					+ "attribute classes of the '/' group will be removed. Error: " + e.toString());
			GroupContents root = groupManagement.getContents("/", GroupContents.METADATA);
			log.info("Removing ACs: " + root.getGroup().getAttributesClasses());
			root.getGroup().setAttributesClasses(new HashSet<>());
			groupManagement.updateGroup("/", root.getGroup(), "reset root group attributes", "");
			return idManagement.addEntity(admin, crDef, EntityState.valid);
		}
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
			for (AuthenticationRealm ar : realms)
				realmManagement.removeRealm(ar.getName());
		} catch (EngineException e)
		{
			log.fatal("Can't remove realms which are stored in database", e);
			throw new InternalException("Can't remove realms which are stored in database", e);
		}

		log.info("Removing all persisted authenticators");
		tx.runInTransaction(() -> {
			authenticatorDAO.deleteAll();
		});
		log.info("Removing all persisted authentication flows");
		tx.runInTransaction(() -> {
			authenticationFlowDAO.deleteAll();
		});

	}

	private void initializeRealms()
	{
		try
		{
			log.info("Loading configured realms");
			Collection<AuthenticationRealm> realms = realmManagement.getRealms();
			Set<String> realmKeys = config.getStructuredListKeys(UnityServerConfiguration.REALMS);
			for (String realmKey : realmKeys)
			{
				String name = config.getValue(realmKey + UnityServerConfiguration.REALM_NAME);
				String description = config
						.getValue(realmKey + UnityServerConfiguration.REALM_DESCRIPTION);
				int blockAfter = config.getIntValue(
						realmKey + UnityServerConfiguration.REALM_BLOCK_AFTER_UNSUCCESSFUL);
				int blockFor = config.getIntValue(realmKey + UnityServerConfiguration.REALM_BLOCK_FOR);
				RememberMePolicy remeberMePolicy = config.getEnumValue(
						realmKey + UnityServerConfiguration.REALM_REMEMBER_ME_POLICY,
						RememberMePolicy.class);
				int remeberMeFor = config
						.getIntValue(realmKey + UnityServerConfiguration.REALM_REMEMBER_ME_FOR);
				int maxInactive = config
						.getIntValue(realmKey + UnityServerConfiguration.REALM_MAX_INACTIVITY);

				AuthenticationRealm realm = new AuthenticationRealm(name, description, blockAfter,
						blockFor, remeberMePolicy, remeberMeFor, maxInactive);

				if (realms.stream().filter(r -> r.getName().equals(name)).findAny().isPresent())
					realmManagement.updateRealm(realm);
				else
					realmManagement.addRealm(realm);

				description = description == null ? "" : description;
				log.info(" - " + name + ": " + description + " [blockAfter " + blockAfter
						+ ", blockFor " + blockFor + ", rememberMePolicy "
						+ remeberMePolicy.toString() + ", rememberMeFor " + remeberMeFor
						+ ", maxInactive " + maxInactive);
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
		logEndpoints();
	}

	private void logEndpoints()
	{
		try
		{
			List<ResolvedEndpoint> endpoints = endpointManager.getDeployedEndpoints();
			log.info("Initialized the following endpoints:");
			for (ResolvedEndpoint endpoint : endpoints)
			{
				log.info(" - " + endpoint.getName() + ": " + endpoint.getType().getName() + " "
						+ endpoint.getEndpoint().getConfiguration().getDescription() + " at "
						+ endpoint.getEndpoint().getContextAddress()
						+ (endpoint.getRealm() == null ? "" : " in realm " + endpoint.getRealm().getName()));
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

		List<ResolvedEndpoint> existing = endpointManager.getDeployedEndpoints();

		Set<String> endpointsList = config.getStructuredListKeys(UnityServerConfiguration.ENDPOINTS);
		for (String endpointKey : endpointsList)
		{
			String description = config
					.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_DESCRIPTION);
			String type = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_TYPE);
			File configFile = config.getFileValue(
					endpointKey + UnityServerConfiguration.ENDPOINT_CONFIGURATION, false);
			String address = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_ADDRESS);
			String name = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_NAME);

			if (existing.stream().filter(e -> e.getName().equals(name)).findAny().isPresent())
			{
				log.info("Endpoint " + name
						+ " is present in database, will be updated from configuration");
				endpointManager.undeploy(name);
			}

			I18nString displayedName = config.getLocalizedString(msg,
					endpointKey + UnityServerConfiguration.ENDPOINT_DISPLAYED_NAME);
			if (displayedName.isEmpty())
				displayedName.setDefaultValue(name);
			String realmName = config.getValue(endpointKey + UnityServerConfiguration.ENDPOINT_REALM);

			List<String> endpointAuthn = config.getEndpointAuth(endpointKey);
			String jsonConfiguration = FileUtils.readFileToString(configFile, Charset.defaultCharset());

			log.info(" - " + name + ": " + type + " " + description);
			EndpointConfiguration endpointConfiguration = new EndpointConfiguration(displayedName,
					description, endpointAuthn, jsonConfiguration, realmName);
			endpointConfiguration.setTag(config.getValue(
					endpointKey + UnityServerConfiguration.ENDPOINT_CONFIGURATION));
			
			endpointManager.deploy(type, name, address, endpointConfiguration);
		}
	}

	private void initializeAuthenticationFlows()
	{
		try
		{
			loadAuthenticationFlowsFromConfiguration();
		} catch (Exception e)
		{
			log.fatal("Can't load authentication flows which are configured", e);
			throw new InternalException("Can't load authentication flows which are configured", e);
		}
	}

	private void loadAuthenticationFlowsFromConfiguration() throws EngineException

	{
		log.info("Loading all configured authentication flows");
		Collection<AuthenticatorInfo> authenticators = authnManagement.getAuthenticators(null);
		Set<String> existinguthenticators = authenticators.stream().map(a -> a.getId())
				.collect(Collectors.toSet());
		Collection<AuthenticationFlowDefinition> authenticationFlows = authnFlowManagement
				.getAuthenticationFlows();
		Map<String, AuthenticationFlowDefinition> existing = new HashMap<>();
		for (AuthenticationFlowDefinition af : authenticationFlows)
			existing.put(af.getName(), af);

		Set<String> authenticationFlowList = config
				.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATION_FLOW);
		for (String authenticationFlowKey : authenticationFlowList)
		{
			String name = config.getValue(
					authenticationFlowKey + UnityServerConfiguration.AUTHENTICATION_FLOW_NAME);

			if (existinguthenticators.contains(name))
				throw new InternalException(
						"Can't add authentication flow which are defined in configuration. The authentication flow name: "
								+ name + " is the same as one of authenticator name");

			Policy policy = config.getEnumValue(
					authenticationFlowKey + UnityServerConfiguration.AUTHENTICATION_FLOW_POLICY,
					Policy.class);
			String firstFactorSpec = config.getValue(authenticationFlowKey
					+ UnityServerConfiguration.AUTHENTICATION_FLOW_FIRST_FACTOR_AUTHENTICATORS);
			String[] firstFactorAuthn = firstFactorSpec.split(",");
			Set<String> firstFactorAuthnSet = new LinkedHashSet<>(Arrays.asList(firstFactorAuthn));

			String secondFactorSpec = config.getValue(authenticationFlowKey
					+ UnityServerConfiguration.AUTHENTICATION_FLOW_SECOND_FACTOR_AUTHENTICATORS);

			List<String> secondFactorAuthnList = new ArrayList<>();
			if (secondFactorSpec != null && !secondFactorSpec.isEmpty())
			{
				String[] secondFactorAuthn = secondFactorSpec.split(",");
				secondFactorAuthnList = Arrays.asList(secondFactorAuthn);
			}

			AuthenticationFlowDefinition authFlowdef = new AuthenticationFlowDefinition(name, policy,
					firstFactorAuthnSet, secondFactorAuthnList);

			if (!existing.containsKey(name))
			{
				authnFlowManagement.addAuthenticationFlow(authFlowdef);
				log.info(" - " + name + " [" + policy.toString() + "]");
			} else
			{
				authnFlowManagement.updateAuthenticationFlow(authFlowdef);
				log.info(" - " + name + " [" + policy.toString() + "] (updated)");
			}
		}

	}

	private void initializeAuthenticators()
	{
		try
		{
			loadAuthenticatorsFromConfiguration();
		} catch (Exception e)
		{
			log.fatal("Can't load authenticators which are configured", e);
			throw new InternalException("Can't load authenticators which are configured", e);
		}
	}

	private void loadAuthenticatorsFromConfiguration() throws IOException, EngineException
	{
		log.info("Loading all configured authenticators");
		Collection<AuthenticatorInfo> authenticators = authnManagement.getAuthenticators(null);
		Map<String, AuthenticatorInfo> existing = new HashMap<>();
		for (AuthenticatorInfo ai : authenticators)
			existing.put(ai.getId(), ai);

		Set<String> authenticatorsList = config.getStructuredListKeys(UnityServerConfiguration.AUTHENTICATORS);
		for (String authenticatorKey : authenticatorsList)
		{
			String name = config.getValue(authenticatorKey + UnityServerConfiguration.AUTHENTICATOR_NAME);
			String type = config.getValue(authenticatorKey + UnityServerConfiguration.AUTHENTICATOR_TYPE);
			File configFile = config.getFileValue(
					authenticatorKey + UnityServerConfiguration.AUTHENTICATOR_VERIFICATOR_CONFIG,
					false);
			String credential = config
					.getValue(authenticatorKey + UnityServerConfiguration.AUTHENTICATOR_CREDENTIAL);

			String configuration = configFile == null ? null
					: FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);

			if (!existing.containsKey(name))
			{
				authnManagement.createAuthenticator(name, type, configuration, credential);
				log.info(" - " + name + " [" + type + "]");
			} else
			{
				authnManagement.updateAuthenticator(name, configuration, credential);
				log.info(" - " + name + " [" + type + "] (updated)");
			}
		}
	}

	private void initializeCredentials()
	{
		try
		{
			loadCredentialsFromConfiguration();
		} catch (Exception e)
		{
			log.fatal("Can't load credentials which are configured", e);
			throw new InternalException("Can't load credentials which are configured", e);
		}
	}

	private void loadCredentialsFromConfiguration() throws IOException, EngineException
	{
		log.info("Loading all configured credentials");
		Collection<CredentialDefinition> definitions = credMan.getCredentialDefinitions();
		Map<String, CredentialDefinition> existing = new HashMap<>();
		for (CredentialDefinition cd : definitions)
			existing.put(cd.getName().toLowerCase(), cd);

		Set<String> credentialsList = config.getStructuredListKeys(UnityServerConfiguration.CREDENTIALS);
		for (String credentialKey : credentialsList)
		{
			String name = config.getValue(credentialKey + UnityServerConfiguration.CREDENTIAL_NAME);
			String typeId = config.getValue(credentialKey + UnityServerConfiguration.CREDENTIAL_TYPE);
			String description = config
					.getValue(credentialKey + UnityServerConfiguration.CREDENTIAL_DESCRIPTION);
			File configFile = config.getFileValue(
					credentialKey + UnityServerConfiguration.CREDENTIAL_CONFIGURATION, false);

			String jsonConfiguration = FileUtils.readFileToString(configFile, Charset.defaultCharset());
			CredentialDefinition credentialDefinition = new CredentialDefinition(typeId, name,
					new I18nString(name), new I18nString(description));
			credentialDefinition.setConfiguration(jsonConfiguration);

			if (!existing.containsKey(name.toLowerCase()))
			{
				credMan.addCredentialDefinition(credentialDefinition);
				log.info(" - " + name + " [" + typeId + "]");
			}
		}
	}

	private void initializeCredentialReqirements()
	{
		try
		{
			loadCredentialRequirementsFromConfiguration();
		} catch (Exception e)
		{
			log.fatal("Can't load configured credential requirements", e);
			throw new InternalException("Can't load configured credential requirements", e);
		}
	}

	private void loadCredentialRequirementsFromConfiguration() throws IOException, EngineException
	{
		log.info("Loading all configured credential requirements");
		Collection<CredentialRequirements> definitions = credReqMan.getCredentialRequirements();
		Map<String, CredentialRequirements> existing = new HashMap<>();
		for (CredentialRequirements cd : definitions)
			existing.put(cd.getName(), cd);

		Set<String> credreqsList = config.getStructuredListKeys(UnityServerConfiguration.CREDENTIAL_REQS);
		for (String credentialKey : credreqsList)
		{
			String name = config.getValue(credentialKey + UnityServerConfiguration.CREDENTIAL_REQ_NAME);
			String description = config
					.getValue(credentialKey + UnityServerConfiguration.CREDENTIAL_REQ_DESCRIPTION);
			List<String> elements = config.getListOfValues(
					credentialKey + UnityServerConfiguration.CREDENTIAL_REQ_CONTENTS);
			Set<String> requiredCredentials = new HashSet<>();
			requiredCredentials.addAll(elements);

			CredentialRequirements cr = new CredentialRequirements(name, description, requiredCredentials);

			if (!existing.containsKey(name))
			{
				credReqMan.addCredentialRequirement(cr);
				log.info(" - " + name + " " + elements.toString());
			}
		}
	}

	private void initializeTranslationProfiles(boolean allowUpdatingExisting)
	{
		List<String> profileFiles = config.getListOfValues(UnityServerConfiguration.TRANSLATION_PROFILES);
		Map<String, TranslationProfile> existingInputProfiles;
		Map<String, TranslationProfile> existingOutputProfiles;
		try
		{
			existingInputProfiles = profilesManagement.listInputProfiles();
			existingOutputProfiles = profilesManagement.listOutputProfiles();
		} catch (EngineException e1)
		{
			throw new InternalException("Can't list the existing translation profiles", e1);
		}
		log.info("Loading configured translation profiles");
		for (String profileFile : profileFiles)
		{
			ObjectNode json;
			try
			{
				String source = FileUtils.readFileToString(new File(profileFile), Charset.defaultCharset());
				json = JsonUtil.parse(source);
			} catch (IOException e)
			{
				throw new ConfigurationException(
						"Problem loading translation profile from file: " + profileFile, e);
			}
			TranslationProfile tp = new TranslationProfile(json);
			try
			{
				if ((tp.getProfileType() == ProfileType.INPUT
						&& existingInputProfiles.containsKey(tp.getName()))
						|| tp.getProfileType() == ProfileType.OUTPUT
								&& existingOutputProfiles.containsKey(tp.getName()))
				{
					if (allowUpdatingExisting)
					{
						profilesManagement.updateProfile(tp);
						log.info(" - updated the in-DB translation profile : " + tp.getName()
							+ " with file definition: " + profileFile);
					}
				} else
				{
					profilesManagement.addProfile(tp);
					log.info(" - loaded translation profile: " + tp.getName() + " from file: "
							+ profileFile);
				}
			} catch (Exception e)
			{
				throw new InternalException(
						"Can't install the configured translation profile " + tp.getName(), e);
			}
		}
	}

	private void checkProfiles(Collection<TranslationProfile> collection)
	{
		for (TranslationProfile profile : collection)
		{
			if (profile.getProfileMode() != ProfileMode.READ_ONLY)
				throw new IllegalArgumentException(
						"System profile " + profile + " is not in READ_ONLY mode");
			profileHelper.checkBaseProfileContent(profile);
		}
	}

	private void checkSystemTranslationProfiles()
	{
		checkProfiles(systemInputProfileProvider.getSystemProfiles().values());
		checkProfiles(systemOutputProfileProvider.getSystemProfiles().values());
	}

	private void runInitializers()
	{
		List<String> enabledL = config.getListOfValues(UnityServerConfiguration.INITIALIZERS);
		Map<String, ServerInitializer> initializersMap = new HashMap<>();
		for (ServerInitializer initializer : initializers.orElseGet(ArrayList::new))
			initializersMap.put(initializer.getName(), initializer);

		for (String enabled : enabledL)
		{
			log.info("Running initializer: " + enabled);
			ServerInitializer serverInitializer = initializersMap.get(enabled);
			if (serverInitializer == null)
				throw new ConfigurationException(
						"There is no content intializer " + enabled + " defined in the system");
			initializersMap.get(enabled).run();
		}
	}
}
