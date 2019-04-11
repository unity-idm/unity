/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.notifications;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.GenericMessageTemplateDef;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.notification.NotificationStatus;
import pl.edu.icm.unity.engine.msgtemplate.MessageTemplateProcessor;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.store.api.MembershipDAO;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.tx.Transactional;
import pl.edu.icm.unity.store.api.tx.TxManager;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageTemplate.Message;

/**
 * Internal (shouldn't be exposed directly to end-users) subsystem for sending notifications.
 * @author K. Benedyczak
 */
@Component
public class NotificationProducerImpl implements NotificationProducer, InternalFacilitiesManagement
{
	private NotificationFacilitiesRegistry facilitiesRegistry;
	private MembershipDAO dbGroups;
	private MessageTemplateDB mtDB;
	private UnityMessageSource msg;
	private TxManager txManager;
	private final MessageTemplateProcessor messageTemplateProcessor = new MessageTemplateProcessor();
	private final ChannelInstanceFactory channelFactory;
	
	@Autowired
	public NotificationProducerImpl(
			ChannelInstanceFactory channelFactory, 
			NotificationFacilitiesRegistry facilitiesRegistry,
			MembershipDAO dbGroups, MessageTemplateDB mtDB, UnityMessageSource msg,
			TxManager txManager)
	{
		this.channelFactory = channelFactory;
		this.dbGroups = dbGroups;
		this.txManager = txManager;
		this.facilitiesRegistry = facilitiesRegistry;
		this.mtDB = mtDB;
		this.msg = msg;
	}
	
	@Transactional(autoCommit=false)
	@Override
	public Future<NotificationStatus> sendNotification(EntityParam recipient, String templateId,
			Map<String, String> params, String locale, String preferredAddress, boolean onlyToConfirmed)
			throws EngineException
	{
		recipient.validateInitialization();

		Map<String, MessageTemplate> allTemplates = mtDB.getAllAsMap();
		NotificationChannelInstance channel = channelFactory.loadChannel(getChannelFromTemplate(allTemplates, templateId));
		NotificationFacility facility = facilitiesRegistry.getByName(channel.getFacilityId());
		String recipientAddress = facility.getAddressForEntity(recipient, preferredAddress, onlyToConfirmed);
		txManager.commit();
		return sendMessageOverChannel(recipientAddress, templateId, params, locale, allTemplates, channel);
	}
	
	@Override
	@Transactional
	public void sendNotificationToGroup(String group, String templateId,
			Map<String, String> params, String locale) throws EngineException
	{
		if (templateId == null)
			return;

		Map<String, MessageTemplate> allTemplates = mtDB.getAllAsMap();

		List<GroupMembership> memberships = dbGroups.getMembers(group);

		NotificationChannelInstance channel = channelFactory.loadChannel(getChannelFromTemplate(allTemplates, templateId));
		NotificationFacility facility = facilitiesRegistry.getByName(channel.getFacilityId());

		for (GroupMembership membership: memberships)
		{
			try
			{
				String recipientAddress = facility.getAddressForEntity(
						new EntityParam(membership.getEntityId()), null, false);
				sendMessageOverChannel(recipientAddress, templateId, params, locale, 
						allTemplates, channel);
			} catch (IllegalIdentityValueException e)
			{
				//OK - ignored
			}
		}
	}

	@Override
	@Transactional(autoCommit=false)
	public Future<NotificationStatus> sendNotification(String recipientAddress,
			String templateId, Map<String, String> params, String locale)
			throws EngineException
	{
	
		Map<String, MessageTemplate> allTemplates = mtDB.getAllAsMap();
		NotificationChannelInstance channel = channelFactory.loadChannel(getChannelFromTemplate(allTemplates, templateId));
		txManager.commit();
		return sendMessageOverChannel(recipientAddress, templateId, params, locale, allTemplates, channel);
	}

	
	@Override
	public NotificationFacility getNotificationFacilityForChannel(String channelName) 
			throws EngineException
	{
		NotificationChannelInstance channel = channelFactory.loadChannel(channelName);
		return facilitiesRegistry.getByName(channel.getFacilityId());
	}
	
	@Override
	public NotificationFacility getNotificationFacilityForMessageTemplate(String templateId) 
			throws EngineException
	{
		NotificationFacility notificationFacility = getNotificationFacilityForChannel(
				 mtDB.get(templateId).getNotificationChannel());
		return notificationFacility;
	}
	
	
	public Message getResolvedMessage(Map<String, MessageTemplate> allTemplates, 
			String templateId, Map<String, String> params, String locale)
	{
		MessageTemplate requested = allTemplates.get(templateId);
		if (requested == null)
			throw new IllegalArgumentException("There is no message template " + templateId);
		Map<String, MessageTemplate> genericTemplates = allTemplates.entrySet().stream()
			.filter(e -> e.getValue().getConsumer().equals(GenericMessageTemplateDef.NAME))
			.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
		return messageTemplateProcessor.getMessage(requested, locale, msg.getDefaultLocaleCode(), 
				params, genericTemplates);
	}

	@Override
	@Transactional
	public String getAddressForEntity(EntityParam recipient, String templateId, boolean onlyConfirmed) throws EngineException
	{
		Map<String, MessageTemplate> allTemplates = mtDB.getAllAsMap();
		NotificationChannelInstance channel = channelFactory.loadChannel(getChannelFromTemplate(allTemplates, templateId));
		NotificationFacility facility = facilitiesRegistry.getByName(channel.getFacilityId());
		return facility.getAddressForEntity(recipient, null, onlyConfirmed);
	}
	
	private Future<NotificationStatus> sendMessageOverChannel(String recipientAddress,
			String templateId, Map<String, String> params, String locale, 
			Map<String, MessageTemplate> allTemplates,
			NotificationChannelInstance channel)
	{
		if (channel.providesMessageTemplatingFunctionality())
		{
			return channel.sendExternalTemplateMessage(recipientAddress, 
					new MessageTemplateParams(templateId, params));
		} else
		{
			Message templateMsg = getResolvedMessage(allTemplates, templateId, params, locale);
			return channel.sendNotification(recipientAddress, templateMsg);
		}
	}
	

	private String getChannelFromTemplate(Map<String, MessageTemplate> allTemplates,
			String templateId) throws EngineException
	{
		MessageTemplate messageTemplate = allTemplates.get(templateId);
		if (messageTemplate == null)
			throw new IllegalArgumentException(
					"There is no message template: " + templateId);

		String channel = messageTemplate.getNotificationChannel();

		if (channel == null | channel.isEmpty())
		{
			throw new IllegalArgumentException(
					"There is no configured notification channel in message template: "
							+ templateId);
		}
		return channel;
	}
}
