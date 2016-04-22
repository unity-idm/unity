/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations;

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

import net.sf.ehcache.Cache;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.Searchable;
import net.sf.ehcache.search.Query;
import net.sf.ehcache.search.Results;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder;
import pl.edu.icm.unity.confirmations.ConfirmationRedirectURLBuilder.Status;
import pl.edu.icm.unity.confirmations.ConfirmationServlet;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteConfirmationState;
import pl.edu.icm.unity.db.generic.confirmation.ConfirmationConfigurationDB;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateDB;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.SharedEndpointManagementImpl;
import pl.edu.icm.unity.engine.transactions.SqlSessionTL;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.api.internal.TransactionalRunner;
import pl.edu.icm.unity.server.utils.CacheProvider;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
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
	
	private TokensManagement tokensMan;
	private NotificationProducer notificationProducer;
	private ConfirmationFacilitiesRegistry confirmationFacilitiesRegistry;
	private MessageTemplateDB mtDB;
	private ConfirmationConfigurationDB configurationDB;
	private URL advertisedAddress;
	private UnityMessageSource msg;
	private IdentitiesResolver idResolver;
	private Ehcache confirmationReqCache;
	private int requestLimit;
	private String defaultRedirectURL;
	private TransactionalRunner tx;

	@Autowired
	public ConfirmationManagerImpl(TokensManagement tokensMan,
			MessageTemplateManagement templateMan,
			NotificationProducer notificationProducer,
			ConfirmationFacilitiesRegistry confirmationFacilitiesRegistry,
			JettyServer httpServer, MessageTemplateDB mtDB,
			ConfirmationConfigurationDB configurationDB, TransactionalRunner tx,
			IdentitiesResolver idResolver, UnityMessageSource msg,
			CacheProvider cacheProvider, UnityServerConfiguration mainConf)
	{
		this.tokensMan = tokensMan;
		this.notificationProducer = notificationProducer;
		this.confirmationFacilitiesRegistry = confirmationFacilitiesRegistry;
		this.tx = tx;
		this.idResolver = idResolver;
		this.advertisedAddress = httpServer.getAdvertisedAddress();
		this.mtDB = mtDB;
		this.configurationDB = configurationDB;
		this.msg = msg;
		
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
		ConfirmationFacility<?> facility = getFacility(facilityId);
		ConfirmationConfiguration configEntry = getConfiguration(baseState);
		if (configEntry == null)
			return;
		String serializedState = baseState.getSerializedConfiguration();
		
		if (!checkSendingLimit(baseState.getValue()))
			return;
			
		TokenAndFlag taf = tx.runInTransactionRet(() -> {
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
				+ ConfirmationServlet.SERVLET_PATH;
		HashMap<String, String> params = new HashMap<>();
		params.put(ConfirmationTemplateDef.CONFIRMATION_LINK, link + "?"
				+ ConfirmationServlet.CONFIRMATION_TOKEN_ARG + "=" + token);

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
		
		return tx.runInTransactionRet(() -> {
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
		ConfirmationFacility<?> facility = getFacility(baseState.getFacilityId());
		tokensMan.removeToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk.getValue());
		log.debug("Process confirmation using " + facility.getName() + " facility");
		ConfirmationStatus status = facility.processConfirmation(rawState, SqlSessionTL.get());

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
	
	
	
	private <T> void sendVerification(EntityParam entity, Attribute<T> attribute, boolean force) 
			throws EngineException
	{
		if (!attribute.getAttributeSyntax().isVerifiable())
			return;
		for (T valA : attribute.getValues())
		{
			VerifiableElement val = (VerifiableElement) valA;
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
		return tx.runInTransactionRet(() -> {
			return idResolver.getEntityId(entity, SqlSessionTL.get());
		});
	}
	
	@Override
	public <T> void sendVerificationQuiet(EntityParam entity, Attribute<T> attribute, boolean force)
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
	public void sendVerificationsQuiet(EntityParam entity, Collection<? extends Attribute<?>> attributes, boolean force)
	{
		for (Attribute<?> attribute: attributes)
			sendVerificationQuiet(entity, attribute, force);
	}

	@Override
	public void sendVerification(EntityParam entity, Identity identity, boolean force) 
			throws EngineException
	{
		if (!identity.getType().getIdentityTypeProvider().isVerifiable())
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
			return configurationDB.get(typeToConfirm + nameToConfirm, SqlSessionTL.get());
		});
	}

	private Collection<MessageTemplate> getAllTemplatesFromDB() throws EngineException
	{
		return tx.runInTransactionRet(() -> {
			Map<String, MessageTemplate> templates = mtDB.getAllAsMap(SqlSessionTL.get());
			return templates.values();
		});
	}

	private ConfirmationFacility<?> getFacility(String id) throws InternalException
	{
		ConfirmationFacility<?> facility = null;
		try
		{
			facility = confirmationFacilitiesRegistry.getByName(id);
		} catch (IllegalTypeException e)
		{
			throw new InternalException("Can't find facility with name " + id, e);
		}
		return facility;
	}

}
