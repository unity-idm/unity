/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.integration;

import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.integration.IntegrationEvent;
import pl.edu.icm.unity.engine.api.integration.IntegrationEventProcessor;
import pl.edu.icm.unity.engine.api.integration.Message;
import pl.edu.icm.unity.engine.api.integration.Webhook;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.webhook.WebhookProcessor;

/**
 * Implementation of {@link IntegrationEventProcessor}
 * 
 * @author P.Piernik
 *
 */
@Component
public class IntegrationEventProcessorImpl implements IntegrationEventProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, IntegrationEventProcessorImpl.class);

	private NotificationProducer notificationProducer;
	private WebhookProcessor webhookProcessor;
	private MessageSource msg;

	@Autowired
	IntegrationEventProcessorImpl(NotificationProducer notificationProducer, WebhookProcessor webhookProcessor,
			MessageSource msg)
	{
		this.notificationProducer = notificationProducer;
		this.webhookProcessor = webhookProcessor;
		this.msg = msg;
	}

	@Override
	public void trigger(IntegrationEvent event, Map<String, String> params)
	{
		log.debug("Trigger integration event " + event.name + " with params " + params);

		if (event.type.equals(IntegrationEvent.EventType.MESSAGE))
		{
			sendMessage((Message) event.configuration, params);
		} else if (event.type.equals(IntegrationEvent.EventType.WEBHOOK))
		{
			triggerWebhook((Webhook) event.configuration, params);
		}
	}

	private void triggerWebhook(Webhook webhook, Map<String, String> params)
	{
		try
		{
			webhookProcessor.trigger(webhook, params);
		} catch (EngineException e)
		{
			log.error("Webhook execution error", e);
		}
	}

	private void sendMessage(Message message, Map<String, String> params)
	{
		try
		{
			notificationProducer.sendNotification(message.groupsRecipients, message.singleRecipients,
					message.messageTemplate, params, msg.getLocaleCode());
		} catch (EngineException e)
		{
			log.error("Can not send message", e);
		}
	}

}
