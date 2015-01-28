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

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.ConfirmationServlet;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.confirmations.states.AttribiuteConfirmationState;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.confirmations.states.IdentityConfirmationState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteConfirmationState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.confirmation.ConfirmationConfigurationDB;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateDB;
import pl.edu.icm.unity.db.resolvers.IdentitiesResolver;
import pl.edu.icm.unity.engine.SharedEndpointManagementImpl;
import pl.edu.icm.unity.engine.notifications.NotificationProducerImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.registries.ConfirmationFacilitiesRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
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

	private TokensManagement tokensMan;
	private NotificationProducerImpl notificationProducer;
	private ConfirmationFacilitiesRegistry confirmationFacilitiesRegistry;
	private MessageTemplateDB mtDB;
	private ConfirmationConfigurationDB configurationDB;
	private DBSessionManager db;
	private URL advertisedAddress;
	private UnityMessageSource msg;
	private IdentitiesResolver idResolver;

	@Autowired
	public ConfirmationManagerImpl(TokensManagement tokensMan,
			MessageTemplateManagement templateMan,
			NotificationProducerImpl notificationProducer,
			ConfirmationFacilitiesRegistry confirmationFacilitiesRegistry,
			JettyServer httpServer, MessageTemplateDB mtDB,
			ConfirmationConfigurationDB configurationDB, DBSessionManager db,
			IdentitiesResolver idResolver,
			UnityMessageSource msg)
	{
		this.tokensMan = tokensMan;
		this.notificationProducer = notificationProducer;
		this.confirmationFacilitiesRegistry = confirmationFacilitiesRegistry;
		this.idResolver = idResolver;
		this.advertisedAddress = httpServer.getAdvertisedAddress();
		this.mtDB = mtDB;
		this.configurationDB = configurationDB;
		this.db = db;
		this.msg = msg;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendConfirmationRequest(BaseConfirmationState baseState) throws EngineException
	{
		String facilityId = baseState.getFacilityId();
		ConfirmationFacility<?> facility = getFacility(facilityId);
		ConfirmationConfiguration configEntry = getConfiguration(baseState);
		if (configEntry == null)
			return;
		String serializedState = baseState.getSerializedConfiguration();
		

		Object transaction = tokensMan.startTokenTransaction();
		try
		{
			boolean hasDuplicate = !getDuplicateTokens(facility, serializedState, transaction).isEmpty();
			String token = insertConfirmationToken(serializedState, transaction);
			if (!hasDuplicate)
				sendConfirmationRequest(baseState.getValue(), configEntry.getNotificationChannel(),
						configEntry.getMsgTemplate(), 
						facility, baseState.getLocale(), token);
			facility.processAfterSendRequest(serializedState);
		} finally
		{
			tokensMan.commitTokenTransaction(transaction);
		}
	}

	private String insertConfirmationToken(String state, Object transaction) throws EngineException
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
					createDate, expires, transaction);
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
			return new ConfirmationStatus(false, null, "ConfirmationStatus.invalidToken");

		Object transaction = tokensMan.startTokenTransaction(); 
		try
		{
			Token tk = null;
			try
			{
				tk = tokensMan.getTokenById(ConfirmationManagerImpl.CONFIRMATION_TOKEN_TYPE, token,
						transaction);
			} catch (WrongArgumentException e)
			{
				return new ConfirmationStatus(false, null, "ConfirmationStatus.invalidToken");
			}

			return processConfirmationIntenral(tk, true, transaction);
		} finally 
		{
			tokensMan.commitTokenTransaction(transaction);
		}
	}

	private ConfirmationStatus processConfirmationIntenral(Token tk, boolean withDuplicates, Object transaction)
			throws EngineException
	{

		Date today = new Date();
		if (tk.getExpires().compareTo(today) < 0)
			return new ConfirmationStatus(false, null, "ConfirmationStatus.expiredToken");

		String rawState = tk.getContentsString();
		BaseConfirmationState baseState = new BaseConfirmationState(rawState);
		ConfirmationFacility<?> facility = getFacility(baseState.getFacilityId());
		tokensMan.removeToken(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, tk.getValue(), transaction);
		log.debug("Process confirmation using " + facility.getName() + " facility");
		ConfirmationStatus status = facility.processConfirmation(rawState);

		if (withDuplicates)
		{
			Collection<Token> duplicateTokens = getDuplicateTokens(facility, rawState, transaction);
			for (Token duplicate: duplicateTokens)
			{
				log.debug("Found duplicte confirmation token " + duplicate.getValue() + 
						" confirming it too");
				processConfirmationIntenral(duplicate, false, transaction);
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
	private Collection<Token> getDuplicateTokens(ConfirmationFacility facility, String baseState, 
			Object transaction)
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
		List<Token> tks = tokensMan.getAllTokens(ConfirmationManager.CONFIRMATION_TOKEN_TYPE, transaction);
		for (Token tk : tks)
		{
			if (facility.isDuplicate(state, tk.getContentsString()))
				ret.add(tk);
		}
		return ret;
	}
	
	
	
	private <T> void sendVerification(EntityParam entity, Attribute<T> attribute, boolean useCurrentReturnUrl) 
			throws EngineException
	{
		if (!attribute.getAttributeSyntax().isVerifiable())
			return;
		String url = getCurrentURL(useCurrentReturnUrl);
		for (T valA : attribute.getValues())
		{
			VerifiableElement val = (VerifiableElement) valA;
			ConfirmationInfo ci = val.getConfirmationInfo();
			if (!ci.isConfirmed() && ci.getSentRequestAmount() == 0)
			{
				// TODO - should use user's preferred locale
				long entityId = resolveEntityId(entity);
				AttribiuteConfirmationState state = new AttribiuteConfirmationState(
						entityId,
						attribute.getName(), val.getValue(),
						msg.getDefaultLocaleCode(),
						attribute.getGroupPath(), url, url);
				sendConfirmationRequest(state);
			}
		}
	}

	private long resolveEntityId(EntityParam entity) throws EngineException
	{
		if (entity.getEntityId() != null)
			return entity.getEntityId();
		SqlSession sqlMap = db.getSqlSession(false);
		try
		{
			return idResolver.getEntityId(entity, sqlMap);
		} finally
		{
			db.releaseSqlSession(sqlMap);
		}
	}
	
	@Override
	public <T> void sendVerificationQuiet(EntityParam entity, Attribute<T> attribute, boolean useCurrentReturnUrl)
	{
		try
		{
			sendVerification(entity, attribute, useCurrentReturnUrl);
		} catch (Exception e)
		{
			log.warn("Can not send a confirmation for the verificable attribute being added " + 
					attribute.getName(), e);
		}
	}
	

	@Override
	public void sendVerificationsQuiet(EntityParam entity, List<Attribute<?>> attributes, 
			boolean useCurrentReturnUrl)
	{
		for (Attribute<?> attribute: attributes)
			sendVerificationQuiet(entity, attribute, useCurrentReturnUrl);
	}

	@Override
	public void sendVerification(EntityParam entity, Identity identity, boolean useCurrentReturnUrl) 
			throws EngineException
	{
		if (!identity.getType().getIdentityTypeProvider().isVerifiable())
			return;
		if (identity.isConfirmed())
			return;
		String url = getCurrentURL(useCurrentReturnUrl);
		//TODO - should use user's preferred locale
		IdentityConfirmationState state = new IdentityConfirmationState(
				identity.getEntityId(), identity.getTypeId(),  
				identity.getValue(), msg.getDefaultLocaleCode(),
				url, url);
		sendConfirmationRequest(state);
	}


	@Override
	public void sendVerificationQuiet(EntityParam entity, Identity identity, boolean useCurrentReturnUrl)
	{
		try
		{
			sendVerification(entity, identity, useCurrentReturnUrl);
		} catch (EngineException e)
		{
			log.warn("Can not send a confirmation for the verificable identity being added " + 
					identity.getValue(), e);
		}
	}
	
	private String getCurrentURL(boolean useCurrentReturnUrl)
	{
		if (!useCurrentReturnUrl)
			return null;
		try
		{
			return InvocationContext.getCurrent().getCurrentURLUsed();
		} catch (InternalException e)
		{
			//OK - no context -> no URL.
			return null;
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
		SqlSession sql = db.getSqlSession(true);
		ConfirmationConfiguration configuration = null;
		try
		{
			configuration = configurationDB.get(typeToConfirm + nameToConfirm, sql);
			sql.commit();
			return configuration;
		} finally
		{
			db.releaseSqlSession(sql);
		}
	}

	private Collection<MessageTemplate> getAllTemplatesFromDB() throws EngineException
	{
		SqlSession sql = db.getSqlSession(false);
		try
		{
			Map<String, MessageTemplate> templates = mtDB.getAllAsMap(sql);
			return templates.values();
		} finally
		{
			db.releaseSqlSession(sql);
		}
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
