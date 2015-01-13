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

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.confirmations.ConfirmationFacility;
import pl.edu.icm.unity.confirmations.ConfirmationManager;
import pl.edu.icm.unity.confirmations.ConfirmationServlet;
import pl.edu.icm.unity.confirmations.ConfirmationStatus;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.confirmations.states.BaseConfirmationState;
import pl.edu.icm.unity.confirmations.states.EntityAttribiuteState;
import pl.edu.icm.unity.confirmations.states.RegistrationReqAttribiuteState;
import pl.edu.icm.unity.db.DBSessionManager;
import pl.edu.icm.unity.db.generic.confirmation.ConfirmationConfigurationDB;
import pl.edu.icm.unity.db.generic.msgtemplate.MessageTemplateDB;
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
import pl.edu.icm.unity.server.registries.ConfirmationFacilitiesRegistry;
import pl.edu.icm.unity.server.utils.Log;

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
	private ConfirmationConfigurationDB configurationDB;
	private DBSessionManager db;

	@Autowired
	public ConfirmationManagerImpl(TokensManagement tokensMan,
			MessageTemplateManagement templateMan,
			NotificationProducerImpl notificationProducer,
			ConfirmationFacilitiesRegistry confirmationFacilitiesRegistry,
			JettyServer httpServer, MessageTemplateDB mtDB,
			ConfirmationConfigurationDB configurationDB, DBSessionManager db)
	{
		this.tokensMan = tokensMan;
		this.notificationProducer = notificationProducer;
		this.confirmationFacilitiesRegistry = confirmationFacilitiesRegistry;
		this.advertisedAddress = httpServer.getAdvertisedAddress();
		this.mtDB = mtDB;
		this.configurationDB = configurationDB;
		this.db = db;
	}

	private void sendConfirmationRequest(String recipientAddress, String channelName,
			String templateId, String state, ConfirmationFacility facility)
			throws EngineException
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

		String link = advertisedAddress.toExternalForm()
				+ SharedEndpointManagementImpl.CONTEXT_PATH
				+ ConfirmationServlet.SERVLET_PATH;
		HashMap<String, String> params = new HashMap<>();
		params.put(ConfirmationTemplateDef.CONFIRMATION_LINK, link + "?"
				+ ConfirmationServlet.CONFIRMATION_TOKEN_ARG + "=" + token);

		log.debug("Send confirmation request to " + recipientAddress + " with token = "
				+ token + " and state=" + state);

		notificationProducer.sendNotification(recipientAddress, channelName, templateId,
				params);
		facility.updateSentRequest(state);
	}

	@Override
	public void sendConfirmationRequest(String state) throws EngineException
	{
		BaseConfirmationState baseState = new BaseConfirmationState();
		baseState.setSerializedConfiguration(state);
		String facilityId = baseState.getFacilityId();
		ConfirmationFacility facility = getFacility(baseState.getFacilityId());
		ConfirmationConfiguration configEntry;
		if (facilityId.equals(EntityAttribiuteState.FACILITY_ID)
				|| facilityId.equals(RegistrationReqAttribiuteState.FACILITY_ID))
			configEntry = getConfiguration(
					ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
					baseState.getType());
		else
			configEntry = getConfiguration(
					ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
					baseState.getType());

		sendConfirmationRequest(baseState.getValue(), configEntry.getNotificationChannel(),
				configEntry.getMsgTemplate(), state, facility);

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

	@Override
	public ConfirmationStatus proccessConfirmation(String token) throws EngineException
	{
		if (token == null)
			return new ConfirmationStatus(false, "ConfirmationStatus.invalidToken");

		Token tk = null;
		try
		{
			tk = tokensMan.getTokenById(
					ConfirmationManagerImpl.CONFIRMATION_TOKEN_TYPE, token);
		} catch (WrongArgumentException e)
		{
			log.error("Illegal token used during confirmation", e);
			return new ConfirmationStatus(false, "ConfirmationStatus.invalidToken");
		}

		Date today = new Date();
		if (tk.getExpires().compareTo(today) < 0)
			return new ConfirmationStatus(false, "ConfirmationStatus.expiredToken");

		String rowState = new String(tk.getContents());
		ConfirmationFacility facility = getFacility(rowState);

		ConfirmationStatus status = facility.confirm(rowState);
		tokensMan.removeToken(ConfirmationManagerImpl.CONFIRMATION_TOKEN_TYPE, token);

		return status;
	}

	private ConfirmationFacility getFacility(String state) throws InternalException
	{
		BaseConfirmationState baseState = new BaseConfirmationState();
		baseState.setSerializedConfiguration(state);

		ConfirmationFacility facility = null;
		try
		{
			facility = confirmationFacilitiesRegistry.getByName(baseState
					.getFacilityId());
		} catch (IllegalTypeException e)
		{
			throw new InternalException("Can't find facility with name "
					+ baseState.getFacilityId(), e);
		}
		return facility;
	}
}
