/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.confirmations;

import java.net.URL;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.ibatis.session.SqlSession;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateDB;
import pl.edu.icm.unity.engine.SharedEndpointManagementImpl;
import pl.edu.icm.unity.engine.notifications.NotificationProducerImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;
import pl.edu.icm.unity.server.registries.ConfirmationFacilitiesRegistry;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Confirmation manager
 * 
 * @author P. Piernik
 */
@Component
public class ConfirmationManagerImpl implements ConfirmationManager
{
	private static final Logger log = Log
			.getLogger(Log.U_SERVER, ConfirmationManagerImpl.class);
	
	public static final String CONFIRMATION_TOKEN_TYPE = "Confirmation";
	private TokensManagement tokensMan;
	private NotificationProducerImpl notificationProducer;
	private ConfirmationFacilitiesRegistry confirmationFacilitiesRegistry;
	private URL advertisedAddress;

	private MessageTemplateDB mtDB;
	private DBSessionManager db;

	@Autowired
	public ConfirmationManagerImpl(TokensManagement tokensMan,
			MessageTemplateManagement templateMan,
			NotificationProducerImpl notificationProducer,
			ConfirmationFacilitiesRegistry confirmationFacilitiesRegistry, JettyServer httpServer,
			MessageTemplateDB mtDB, DBSessionManager db)
	{
		this.tokensMan = tokensMan;
		this.notificationProducer = notificationProducer;
		this.confirmationFacilitiesRegistry = confirmationFacilitiesRegistry;
		this.advertisedAddress = httpServer.getAdvertisedAddress();
		this.mtDB = mtDB;
		this.db = db;
	}

	private void sendConfirmationRequest(String recipientAddress, String channelName,
			String templateId, String state) throws EngineException
	{
		Date createDate = new Date();
		Calendar cl = Calendar.getInstance();
		cl.setTime(createDate);
		cl.add(Calendar.HOUR, 48);
		Date expires = cl.getTime();
		String token = UUID.randomUUID().toString();
		try
		{
			tokensMan.addToken(CONFIRMATION_TOKEN_TYPE, token, state.getBytes(),
					createDate, expires);
		} catch (Exception e)
		{
			log.error("Cannot add token to db", e);
			throw e;
		}
		
		MessageTemplate template = null;
		for (MessageTemplate tpl : getAllTemplatesFromDB())
		{
			if (tpl.getName().equals(templateId))
				template = tpl;
		}
		if (!(template != null && template.getConsumer().equals(
				ConfirmationTemplateDef.NAME)))
			throw new WrongArgumentException("Illegal type of template");

		
		// TODO BUILD CONFIRMATION LINK/ CONFIRMATION ENDPOINT
		String link = advertisedAddress.toExternalForm()
				+ SharedEndpointManagementImpl.CONTEXT_PATH + "/confirmation";
		HashMap<String, String> params = new HashMap<>();
		params.put(ConfirmationTemplateDef.CONFIRMATION_LINK, link + "?token=" + token);
	
		log.debug("Send confirmation email to " + recipientAddress + "with token = "
				+ token);

		notificationProducer.sendNotification(recipientAddress, channelName, templateId,
				params);
	}

	@Override
	public void sendConfirmationRequest(String recipientAddress, String type, String state)
			throws EngineException
	{
		// TODO REMOVE SIMPLE INITIALIZE
		HashMap<String, ConfigEntry> configuration = new HashMap<String, ConfigEntry>();
		String template = null;
		for (MessageTemplate tpl : getAllTemplatesFromDB())
		{
			if (tpl.getConsumer().equals(ConfirmationTemplateDef.NAME))
			{
				template = tpl.getName();
				break;
			}

		}

		ConfigEntry entry = new ConfigEntry(UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL,
				template);
		configuration.put(VerifiableEmailAttributeSyntax.ID, entry);

		
		ConfigEntry cfg = configuration.get(type);
		sendConfirmationRequest(recipientAddress, cfg.channel, cfg.template, state);

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
	
	private class ConfigEntry
	{
		private String channel;
		private String template;

		public ConfigEntry(String channel, String template)
		{
			this.channel = channel;
			this.template = template;
		}
	}
	
	@Override
	public ConfirmationStatus proccessConfirmation(String token) throws EngineException
	{

		Token tk = null;
		try
		{
			tk = tokensMan.getTokenById(ConfirmationManagerImpl.CONFIRMATION_TOKEN_TYPE, token);
		} catch (WrongArgumentException e)
		{
			log.error("Illegal token");
			throw e;
		}

		Date today = new Date();
		if (tk.getExpires().compareTo(today) < 0)
			throw new WrongArgumentException("Token expired");

		String rowState = new String(tk.getContents());
		BaseConfirmationState state = new BaseConfirmationState();
		state.setSerializedConfiguration(rowState);
		
		ConfirmationFacility facility = null;
		try
		{
			facility = confirmationFacilitiesRegistry.getByName(state.getFacilityId());
		} catch (IllegalTypeException e)
		{
			throw new InternalException("Can't find verificator", e);
		}
		
		tokensMan.removeToken(ConfirmationManagerImpl.CONFIRMATION_TOKEN_TYPE, token);
		
		return facility.confirm(rowState);

	}

}
