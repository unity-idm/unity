/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmation;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Results;
import pl.edu.icm.unity.base.msgtemplates.confirm.EmailConfirmationTemplateDef;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationRedirectURLBuilder.Status;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationServletProvider;
import pl.edu.icm.unity.engine.api.confirmation.states.BaseEmailConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.EmailAttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.EmailIdentityConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationReqEmailAttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeDefinition;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.endpoint.SharedEndpointManagementImpl;
import pl.edu.icm.unity.engine.identity.IdentityTypeHelper;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;

/**
 * Email confirmation manager, send or process confirmation request
 * 
 * @author P. Piernik
 */
@Component
public class EmailConfirmationManagerImpl implements EmailConfirmationManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, EmailConfirmationManagerImpl.class);
	private static final String CACHE_ID = "EmailConfirmationCache";
	
	private IdentityTypeHelper idTypeHelper;
	private AttributeTypeHelper atTypeHelper;
	private TokensManagement tokensMan;
	private NotificationProducer notificationProducer;
	private EmailConfirmationFacilitiesRegistry confirmationFacilitiesRegistry;
	private MessageTemplateDB mtDB;
	private URL advertisedAddress;
	private UnityMessageSource msg;
	private EntityResolver idResolver;
	private Ehcache confirmationReqCache;
	private int requestLimit;
	private String defaultRedirectURL;
	private TransactionalRunner tx;

	@Autowired
	public EmailConfirmationManagerImpl(IdentityTypeHelper idTypeHelper,
			AttributeTypeHelper atTypeHelper, TokensManagement tokensMan,
			NotificationProducer notificationProducer,
			EmailConfirmationFacilitiesRegistry confirmationFacilitiesRegistry,
			MessageTemplateDB mtDB, NetworkServer server, 
			UnityMessageSource msg, EntityResolver idResolver,
			TransactionalRunner tx, CacheProvider cacheProvider, UnityServerConfiguration mainConf)
	{
		this.idTypeHelper = idTypeHelper;
		this.atTypeHelper = atTypeHelper;
		this.tokensMan = tokensMan;
		this.notificationProducer = notificationProducer;
		this.confirmationFacilitiesRegistry = confirmationFacilitiesRegistry;
		this.mtDB = mtDB;
		this.advertisedAddress = server.getAdvertisedAddress();
		this.msg = msg;
		this.idResolver = idResolver;
		this.tx = tx;
		
		CacheConfiguration cacheConfig = new CacheConfiguration(CACHE_ID, 0);
		Searchable searchable = new Searchable();
		searchable.values(true);
		cacheConfig.addSearchable(searchable);		
		cacheConfig.setTimeToIdleSeconds(24 * 3600);
		cacheConfig.setTimeToLiveSeconds(24 * 3600);
		cacheConfig.setEternal(false);
		PersistenceConfiguration persistCfg = new PersistenceConfiguration();
		persistCfg.setStrategy("none");
		cacheConfig.persistence(persistCfg);		
		confirmationReqCache = cacheProvider.getManager().addCacheIfAbsent(new Cache(cacheConfig));
		requestLimit = mainConf.getEmailConfirmationRequestLimit();
		defaultRedirectURL = mainConf.getValue(UnityServerConfiguration.CONFIRMATION_DEFAULT_RETURN_URL);
	}

	@Override
	public void sendConfirmationRequest(BaseEmailConfirmationState baseState) throws EngineException
	{
		sendConfirmationRequest(baseState, false);
	}
	
	private void sendConfirmationRequest(BaseEmailConfirmationState baseState, boolean force) throws EngineException
	{
		class TokenAndFlag
		{
			private String token;
			private boolean hasDuplicate;
			public TokenAndFlag(String token, boolean hasDuplicate)
			{
				this.token = token;
				this.hasDuplicate = hasDuplicate;
			}
		}
		
		String facilityId = baseState.getFacilityId();
		EmailConfirmationFacility<?> facility = confirmationFacilitiesRegistry.getByName(facilityId);
		Optional<EmailConfirmationConfiguration> configEntry = getConfiguration(baseState);
		if (!configEntry.isPresent()) 
		{
			log.debug("Cannot get confirmation configuration for "
					+ baseState.getType()
					+ ", skiping sendig confirmation request to "
					+ baseState.getValue());
			return;
		}
			
		String serializedState = baseState.getSerializedConfiguration();
		
		if (configEntry.get().getMessageTemplate() == null)
		{
			log.debug("Not sending an email as there is no message template configured for {}", 
					baseState.getValue());
			return;
		}
		
		if (!checkSendingLimit(baseState.getValue()))
			return;
			
		TokenAndFlag taf = tx.runInTransactionRetThrowing(() -> {
			boolean hasDuplicate = !getDuplicateTokens(facility, serializedState).isEmpty();
			String token = insertConfirmationToken(serializedState, 
					configEntry.get().getValidityTime());
			return new TokenAndFlag(token, hasDuplicate);
		});
		
		if (force || !taf.hasDuplicate)
		{
			sendConfirmationRequest(baseState.getValue(),
					configEntry.get().getMessageTemplate(), 
					facility, baseState.getLocale(), taf.token);
			
		} else
		{
			log.debug("Not sending a confirmation message to " + baseState.getValue() + 
					" as such confirmation was already sent");
		}
		facility.processAfterSendRequest(serializedState);
	}

	private boolean checkSendingLimit(String address)
	{
		confirmationReqCache.evictExpiredElements();
		Results results = confirmationReqCache.createQuery().includeValues().addCriteria(
				Query.VALUE.ilike(address)).execute();
		if (results.size() >= requestLimit)
		{		
			log.warn("Limit of sent confirmation requests to email " + address + 
					" was reached. (Limit=" +requestLimit + "/24H)");
			return false;
		}
		return true;
	}
	
	private String insertConfirmationToken(String state, int confirmationValidity) throws EngineException
	{
		Date createDate = new Date();
		Calendar cl = Calendar.getInstance();
		cl.setTime(createDate);
		cl.add(Calendar.MINUTE, confirmationValidity);
		Date expires = cl.getTime();
		String token = UUID.randomUUID().toString();
		try
		{
			tokensMan.addToken(CONFIRMATION_TOKEN_TYPE, token, 
					state.getBytes(StandardCharsets.UTF_8),
					createDate, expires);
		} catch (Exception e)
		{
			log.error("Cannot add token to db", e);
			throw e;
		}
		return token;
	}
	
	private void sendConfirmationRequest(String recipientAddress,
			String templateId, EmailConfirmationFacility<?> facility, String locale, String token)
			throws EngineException
	{
		MessageTemplate template = null;
		for (MessageTemplate tpl : getAllTemplatesFromDB())
		{
			if (tpl.getName().equals(templateId))
				template = tpl;
		}
		
		if (template == null)
			throw new IllegalArgumentException(
					"The message template " + templateId + " does not exists");
		
		if (!(template.getConsumer().equals(
				EmailConfirmationTemplateDef.NAME)))
			throw new IllegalArgumentException(
					"Illegal type of template. Only message templates with "
							+ EmailConfirmationTemplateDef.NAME
							+ " are allowed");

		String link = advertisedAddress.toExternalForm()
				+ SharedEndpointManagementImpl.CONTEXT_PATH
				+ EmailConfirmationServletProvider.SERVLET_PATH;
		HashMap<String, String> params = new HashMap<>();
		params.put(EmailConfirmationTemplateDef.CONFIRMATION_LINK, link + "?"
				+ EmailConfirmationServletProvider.CONFIRMATION_TOKEN_ARG + "=" + token);

		log.debug("Send confirmation request to " + recipientAddress + " with token = "
				+ token);

		confirmationReqCache.put(new Element(token, recipientAddress));
		
		notificationProducer.sendNotification(recipientAddress, templateId,
				params, locale);
	}

	@Override
	public WorkflowFinalizationConfiguration processConfirmation(String token)
			throws EngineException
	{
		if (token == null)
		{
			String redirectURL = new EmailConfirmationRedirectURLBuilder(defaultRedirectURL, 
					Status.elementConfirmationError).
					setErrorCode("noToken").build();
			return WorkflowFinalizationConfiguration.basicError(
					msg.getMessage("ConfirmationStatus.invalidToken"), redirectURL); 
		}
		
		return tx.runInTransactionRetThrowing(() -> {
			Token tk = null;
			try
			{
				tk = tokensMan.getTokenById(EmailConfirmationManagerImpl.CONFIRMATION_TOKEN_TYPE, token);
			} catch (IllegalArgumentException e)
			{
				String redirectURL = new EmailConfirmationRedirectURLBuilder(defaultRedirectURL, 
						Status.elementConfirmationError).
						setErrorCode("invalidToken").build();
				return WorkflowFinalizationConfiguration.basicError(
						msg.getMessage("ConfirmationStatus.invalidToken"), redirectURL);
			}

			return processConfirmationInternal(tk, true);
		});
	}

	private WorkflowFinalizationConfiguration processConfirmationInternal(Token tk, boolean withDuplicates)
			throws EngineException
	{
		Date today = new Date();
		if (tk.getExpires().compareTo(today) < 0)
		{
			String redirectURL = new EmailConfirmationRedirectURLBuilder(defaultRedirectURL, 
					Status.elementConfirmationError).
					setErrorCode("expiredToken").build();
			return WorkflowFinalizationConfiguration.basicError(
					msg.getMessage("ConfirmationStatus.expiredToken"), redirectURL);
		}
		String rawState = tk.getContentsString();
		BaseEmailConfirmationState baseState = new BaseEmailConfirmationState(rawState);
		EmailConfirmationFacility<?> facility = confirmationFacilitiesRegistry.getByName(baseState.getFacilityId());
		tokensMan.removeToken(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk.getValue());
		log.debug("Process confirmation using " + facility.getName() + " facility");
		WorkflowFinalizationConfiguration status = facility.processConfirmation(rawState);
		
		if (withDuplicates)
		{
			Collection<Token> duplicateTokens = getDuplicateTokens(facility, rawState);
			for (Token duplicate: duplicateTokens)
			{
				log.debug("Found duplicte confirmation token " + duplicate.getValue() + 
						" confirming it too");
				processConfirmationInternal(duplicate, false);
			}
		}
		
		return status;
	}

	/**
	 * Searches the available confirmations for other which are for the same user/registration request
	 * as the base token and have the same email value. 
	 * @param base
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection<Token> getDuplicateTokens(EmailConfirmationFacility facility, String baseState)
	{
		BaseEmailConfirmationState state;
		try
		{
			state = facility.parseState(baseState);
		} catch (IllegalArgumentException e)
		{
			throw new InternalException("Bug: token can not be parsed by its own facility", e);
		}
		
		Set<Token> ret = new HashSet<>();
		List<Token> tks = tokensMan.getAllTokens(EmailConfirmationManager.CONFIRMATION_TOKEN_TYPE);
		for (Token tk : tks)
		{
			if (facility.isDuplicate(state, tk.getContentsString()))
				ret.add(tk);
		}
		return ret;
	}
	
	
	
	private void sendVerification(EntityParam entity, Attribute attribute, boolean force) 
			throws EngineException
	{
		AttributeValueSyntax<?> syntax = atTypeHelper.getUnconfiguredSyntax(attribute.getValueSyntax());
		if (!syntax.isEmailVerifiable())
			return;
		for (String valA : attribute.getValues())
		{
			VerifiableElement val = (VerifiableElement) syntax.convertFromString(valA);
			ConfirmationInfo ci = val.getConfirmationInfo();
			if (!ci.isConfirmed() && (force || ci.getSentRequestAmount() == 0))
			{
				// TODO - should use user's preferred locale
				long entityId = resolveEntityId(entity);
				EmailAttribiuteConfirmationState state = new EmailAttribiuteConfirmationState(
						entityId,
						attribute.getName(), val.getValue(),
						msg.getDefaultLocaleCode(),
						attribute.getGroupPath());
				sendConfirmationRequest(state, force);
			}
		}
	}

	private long resolveEntityId(EntityParam entity) throws EngineException
	{
		if (entity.getEntityId() != null)
			return entity.getEntityId();
		return tx.runInTransactionRetThrowing(() -> {
			return idResolver.getEntityId(entity);
		});
	}
	
	@Override
	public void sendVerificationQuietNoTx(EntityParam entity, Attribute attribute, boolean force)
	{
		try
		{
			sendVerification(entity, attribute, force);
		} catch (Exception e)
		{
			log.warn("Can not send a confirmation for the verificable attribute being added "
					+ attribute.getName(), e);
		}
	}
	
	@Override
	public void sendVerificationsQuietNoTx(EntityParam entity, Collection<? extends Attribute> attributes, boolean force)
	{
		for (Attribute attribute: attributes)
			sendVerificationQuietNoTx(entity, attribute, force);
	}

	@Override
	public void sendVerificationNoTx(EntityParam entity, Identity identity, boolean force) 
			throws EngineException
	{
		IdentityTypeDefinition typeDefinition = idTypeHelper.getTypeDefinition(identity.getTypeId());
		if (!typeDefinition.isEmailVerifiable())
			return;
		if (identity.isConfirmed() || (!force && identity.getConfirmationInfo().getSentRequestAmount() > 0))
			return;
		//TODO - should use user's preferred locale
		EmailIdentityConfirmationState state = new EmailIdentityConfirmationState(
				identity.getEntityId(), identity.getTypeId(),  
				identity.getValue(), msg.getDefaultLocaleCode());
		sendConfirmationRequest(state, force);
	}


	@Override
	public void sendVerificationQuietNoTx(EntityParam entity, Identity identity, boolean force)
	{
		try
		{
			sendVerificationNoTx(entity, identity, force);
		} catch (Exception e)
		{
			log.warn("Can not send a confirmation for the verificable identity being added "
					+ identity.getValue(), e);
		}
	}
	
	private Optional<EmailConfirmationConfiguration> getConfiguration(BaseEmailConfirmationState baseState)
	{
		String facilityId = baseState.getFacilityId();
		try
		{
			if (facilityId.equals(EmailAttribiuteConfirmationState.FACILITY_ID)
					|| facilityId.equals(RegistrationReqEmailAttribiuteConfirmationState.FACILITY_ID))
			{
				return getConfirmationConfigurationForAttribute(baseState.getType());
			}
			else
			{
				return Optional.ofNullable(idTypeHelper.getIdentityType(baseState.getType()).getEmailConfirmationConfiguration());	
			}
				
		} catch (Exception e)
		{	
			log.error("Cannot get confirmation configuration", e);
			return Optional.empty();
		}
	}
	
	private Collection<MessageTemplate> getAllTemplatesFromDB() throws EngineException
	{
		return tx.runInTransactionRet(() -> {
			Map<String, MessageTemplate> templates = mtDB.getAllAsMap();
			return templates.values();
		});
	}

	@Override
	public <T> void sendVerification(EntityParam entity, Attribute attribute) throws EngineException
	{
		tx.runInTransactionThrowing(() -> {
			sendVerification(entity, attribute, true);
		});
	}

	@Override
	public void sendVerification(EntityParam entity, Identity identity) throws EngineException
	{
		tx.runInTransactionThrowing(() -> {
			sendVerificationNoTx(entity, identity, true);
		});
	}
	
	@Override
	public Optional<EmailConfirmationConfiguration> getConfirmationConfigurationForAttribute(
			String attributeName)
	{	
		try
		{
			AttributeValueSyntax<?> syntax = atTypeHelper.getSyntax(
					atTypeHelper.getTypeForAttributeName(attributeName));
			if (!syntax.isEmailVerifiable())
				throw new IllegalArgumentException("Unsupported attribute type: "
						+ attributeName + " for email confirmation");
			return syntax.getEmailConfirmationConfiguration();

		} catch (Exception e)
		{
			log.debug("Cannot get confirmation configuration for attribute "
					+ attributeName, e);
			return Optional.empty();
		}
	}
}
