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
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
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
import pl.edu.icm.unity.base.msgtemplates.confirm.ConfirmationTemplateDef;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationManager;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationRedirectURLBuilder.Status;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationServletProvider;
import pl.edu.icm.unity.engine.api.confirmation.ConfirmationStatus;
import pl.edu.icm.unity.engine.api.confirmation.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.BaseConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.IdentityConfirmationState;
import pl.edu.icm.unity.engine.api.confirmation.states.RegistrationReqAttribiuteConfirmationState;
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
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.store.api.generic.ConfirmationConfigurationDB;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.confirmation.ConfirmationConfiguration;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.confirmation.VerifiableElement;

/**
 * Confirmation manager, send or process confirmation request
 * 
 * @author P. Piernik
 */
@Component
public class ConfirmationManagerImpl implements ConfirmationManager
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, ConfirmationManagerImpl.class);
	private static final String CACHE_ID = "ConfirmationCache";
	
	private IdentityTypeHelper idTypeHelper;
	private AttributeTypeHelper atTypeHelper;
	private TokensManagement tokensMan;
	private NotificationProducer notificationProducer;
	private ConfirmationFacilitiesRegistry confirmationFacilitiesRegistry;
	private MessageTemplateDB mtDB;
	private ConfirmationConfigurationDB configurationDB;
	private URL advertisedAddress;
	private UnityMessageSource msg;
	private EntityResolver idResolver;
	private Ehcache confirmationReqCache;
	private int requestLimit;
	private String defaultRedirectURL;
	private TransactionalRunner tx;

	@Autowired
	public ConfirmationManagerImpl(IdentityTypeHelper idTypeHelper,
			AttributeTypeHelper atTypeHelper, TokensManagement tokensMan,
			NotificationProducer notificationProducer,
			ConfirmationFacilitiesRegistry confirmationFacilitiesRegistry,
			MessageTemplateDB mtDB, ConfirmationConfigurationDB configurationDB,
			NetworkServer server, UnityMessageSource msg, EntityResolver idResolver,
			Ehcache confirmationReqCache, int requestLimit, String defaultRedirectURL,
			TransactionalRunner tx, CacheProvider cacheProvider, UnityServerConfiguration mainConf)
	{
		this.idTypeHelper = idTypeHelper;
		this.atTypeHelper = atTypeHelper;
		this.tokensMan = tokensMan;
		this.notificationProducer = notificationProducer;
		this.confirmationFacilitiesRegistry = confirmationFacilitiesRegistry;
		this.mtDB = mtDB;
		this.configurationDB = configurationDB;
		this.advertisedAddress = server.getAdvertisedAddress();
		this.msg = msg;
		this.idResolver = idResolver;
		this.confirmationReqCache = confirmationReqCache;
		this.requestLimit = requestLimit;
		this.defaultRedirectURL = defaultRedirectURL;
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
		requestLimit = mainConf.getIntValue(UnityServerConfiguration.CONFIRMATION_REQUEST_LIMIT);
		defaultRedirectURL = mainConf.getValue(UnityServerConfiguration.CONFIRMATION_DEFAULT_RETURN_URL);
	}

	@Override
	public void sendConfirmationRequest(BaseConfirmationState baseState) throws EngineException
	{
		sendConfirmationRequest(baseState, false);
	}
	
	private void sendConfirmationRequest(BaseConfirmationState baseState, boolean force) throws EngineException
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
		ConfirmationFacility<?> facility = confirmationFacilitiesRegistry.getByName(facilityId);
		ConfirmationConfiguration configEntry = getConfiguration(baseState);
		if (configEntry == null)
			return;
		String serializedState = baseState.getSerializedConfiguration();
		
		if (!checkSendingLimit(baseState.getValue()))
			return;
			
		TokenAndFlag taf = tx.runInTransactionRetThrowing(() -> {
			boolean hasDuplicate = !getDuplicateTokens(facility, serializedState).isEmpty();
			String token = insertConfirmationToken(serializedState);
			return new TokenAndFlag(token, hasDuplicate);
		});
		
		if (force || !taf.hasDuplicate)
		{
			sendConfirmationRequest(baseState.getValue(), configEntry.getNotificationChannel(),
					configEntry.getMsgTemplate(), 
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
			log.warn("Limit of sent confirmation requests to " + address + 
					" was reached. (Limit=" +requestLimit + "/24H)");
			return false;
		}
		return true;
	}
	
	private String insertConfirmationToken(String state) throws EngineException
	{
		Date createDate = new Date();
		Calendar cl = Calendar.getInstance();
		cl.setTime(createDate);
		cl.add(Calendar.HOUR, 48);
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
	
	private void sendConfirmationRequest(String recipientAddress, String channelName,
			String templateId, ConfirmationFacility<?> facility, String locale, String token)
			throws EngineException
	{
		MessageTemplate template = null;
		for (MessageTemplate tpl : getAllTemplatesFromDB())
		{
			if (tpl.getName().equals(templateId))
				template = tpl;
		}
		if (!(template != null && template.getConsumer().equals(
				ConfirmationTemplateDef.NAME)))
			throw new WrongArgumentException("Illegal type of template");

		String link = advertisedAddress.toExternalForm()
				+ SharedEndpointManagementImpl.CONTEXT_PATH
				+ ConfirmationServletProvider.SERVLET_PATH;
		HashMap<String, String> params = new HashMap<>();
		params.put(ConfirmationTemplateDef.CONFIRMATION_LINK, link + "?"
				+ ConfirmationServletProvider.CONFIRMATION_TOKEN_ARG + "=" + token);

		log.debug("Send confirmation request to " + recipientAddress + " with token = "
				+ token);

		confirmationReqCache.put(new Element(token, recipientAddress));
		
		notificationProducer.sendNotification(recipientAddress, channelName, templateId,
				params, locale);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConfirmationStatus processConfirmation(String token)
			throws EngineException
	{
		if (token == null)
		{
			String redirectURL = new ConfirmationRedirectURLBuilder(defaultRedirectURL, 
					Status.elementConfirmationError).
					setErrorCode("noToken").build();
			return new ConfirmationStatus(false, redirectURL, "ConfirmationStatus.invalidToken");
		}
		
		return tx.runInTransactionRetThrowing(() -> {
			Token tk = null;
			try
			{
				tk = tokensMan.getTokenById(ConfirmationManagerImpl.CONFIRMATION_TOKEN_TYPE, token);
			} catch (WrongArgumentException e)
			{
				String redirectURL = new ConfirmationRedirectURLBuilder(defaultRedirectURL, 
						Status.elementConfirmationError).
						setErrorCode("invalidToken").build();
				return new ConfirmationStatus(false, redirectURL, "ConfirmationStatus.invalidToken");
			}

			return processConfirmationInternal(tk, true);
		});
	}

	private ConfirmationStatus processConfirmationInternal(Token tk, boolean withDuplicates)
			throws EngineException
	{
		Date today = new Date();
		if (tk.getExpires().compareTo(today) < 0)
		{
			String redirectURL = new ConfirmationRedirectURLBuilder(defaultRedirectURL, 
					Status.elementConfirmationError).
					setErrorCode("expiredToken").build();
			return new ConfirmationStatus(false, redirectURL, "ConfirmationStatus.expiredToken");
		}
		String rawState = tk.getContentsString();
		BaseConfirmationState baseState = new BaseConfirmationState(rawState);
		ConfirmationFacility<?> facility = confirmationFacilitiesRegistry.getByName(baseState.getFacilityId());
		tokensMan.removeToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk.getValue());
		log.debug("Process confirmation using " + facility.getName() + " facility");
		ConfirmationStatus status = facility.processConfirmation(rawState);

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
	private Collection<Token> getDuplicateTokens(ConfirmationFacility facility, String baseState)
	{
		BaseConfirmationState state;
		try
		{
			state = facility.parseState(baseState);
		} catch (WrongArgumentException e)
		{
			throw new InternalException("Bug: token can not be parsed by its own facility", e);
		}
		
		Set<Token> ret = new HashSet<>();
		List<Token> tks = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE);
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
		AttributeValueSyntax<?> syntax = atTypeHelper.getUnconfiguredSyntax(attribute.getName());
		if (!syntax.isVerifiable())
			return;
		for (String valA : attribute.getValues())
		{
			VerifiableElement val = (VerifiableElement) syntax.convertFromString(valA);
			ConfirmationInfo ci = val.getConfirmationInfo();
			if (!ci.isConfirmed() && (force || ci.getSentRequestAmount() == 0))
			{
				// TODO - should use user's preferred locale
				long entityId = resolveEntityId(entity);
				AttribiuteConfirmationState state = new AttribiuteConfirmationState(
						entityId,
						attribute.getName(), val.getValue(),
						msg.getDefaultLocaleCode(),
						attribute.getGroupPath(), defaultRedirectURL);
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
	public void sendVerificationQuiet(EntityParam entity, Attribute attribute, boolean force)
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
	public void sendVerificationsQuiet(EntityParam entity, Collection<? extends Attribute> attributes, boolean force)
	{
		for (Attribute attribute: attributes)
			sendVerificationQuiet(entity, attribute, force);
	}

	@Override
	public void sendVerification(EntityParam entity, Identity identity, boolean force) 
			throws EngineException
	{
		IdentityTypeDefinition typeDefinition = idTypeHelper.getTypeDefinition(identity.getTypeId());
		if (!typeDefinition.isVerifiable())
			return;
		if (identity.isConfirmed() || (!force && identity.getConfirmationInfo().getSentRequestAmount() > 0))
			return;
		//TODO - should use user's preferred locale
		IdentityConfirmationState state = new IdentityConfirmationState(
				identity.getEntityId(), identity.getTypeId(),  
				identity.getValue(), msg.getDefaultLocaleCode(),
				defaultRedirectURL);
		sendConfirmationRequest(state, force);
	}


	@Override
	public void sendVerificationQuiet(EntityParam entity, Identity identity, boolean force)
	{
		try
		{
			sendVerification(entity, identity, force);
		} catch (Exception e)
		{
			log.warn("Can not send a confirmation for the verificable identity being added "
					+ identity.getValue(), e);
		}
	}
	
	private ConfirmationConfiguration getConfiguration(BaseConfirmationState baseState)
	{
		String facilityId = baseState.getFacilityId();
		try
		{
			if (facilityId.equals(AttribiuteConfirmationState.FACILITY_ID)
					|| facilityId.equals(RegistrationReqAttribiuteConfirmationState.FACILITY_ID))
				return getConfiguration(
						ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
						baseState.getType());
			else
				return getConfiguration(
						ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
						baseState.getType());

		} catch (Exception e)
		{
			log.debug("Cannot get confirmation configuration for "
					+ baseState.getType()
					+ ", skiping sendig confirmation request to "
					+ baseState.getValue());
			return null;
		}
	}
	
	private ConfirmationConfiguration getConfiguration(String typeToConfirm,
			String nameToConfirm) throws EngineException
	{
		return tx.runInTransactionRet(() -> {
			return configurationDB.get(typeToConfirm + nameToConfirm);
		});
	}

	private Collection<MessageTemplate> getAllTemplatesFromDB() throws EngineException
	{
		return tx.runInTransactionRet(() -> {
			Map<String, MessageTemplate> templates = mtDB.getAllAsMap();
			return templates.values();
		});
	}
}
