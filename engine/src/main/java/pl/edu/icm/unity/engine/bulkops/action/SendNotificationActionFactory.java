/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.bulkops.action;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.msg_template.UserNotificationTemplateDef;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationActionType;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition.Type;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.bulkops.EntityAction;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;

/**
 * Allows for sending a message to entity.
 * 
 * @author K. Benedyczak
 */
@Component
public class SendNotificationActionFactory extends AbstractEntityActionFactory
{
	public static final String NAME = "sendMessage";
	private EntityManagement idsMan;
	private NotificationProducer notificationProducer;
	private MessageSource msg;
	
	@Autowired
	public SendNotificationActionFactory(@Qualifier("insecure") EntityManagement idsMan,
			NotificationProducer notificationProducer, MessageSource msg)
	{
		super(NAME, new ActionParameterDefinition[] {
				new ActionParameterDefinition(
						"message",
						"EntityAction.sendMessage.paramDesc.message",
						Type.USER_MESSAGE_TEMPLATE, true)
		});
		this.idsMan = idsMan;
		this.notificationProducer = notificationProducer;
		this.msg = msg;
	}

	@Override
	public EntityAction getInstance(String... parameters)
	{
		return new SendNotificationAction(idsMan, notificationProducer, msg, 
				getActionType(), parameters);
	}

	public static class SendNotificationAction extends EntityAction
	{
		private static final Logger log = Log.getLogger(Log.U_SERVER_BULK_OPS,
				SendNotificationAction.class);
		private NotificationProducer notificationProducer;
		private String template;
		private MessageSource msg;
		private EntityManagement idsMan;
		
		public SendNotificationAction(EntityManagement idsMan,
				NotificationProducer notificationProducer,
				MessageSource msg,
				TranslationActionType description, String[] params)
		{
			super(description, params);
			this.idsMan = idsMan;
			this.notificationProducer = notificationProducer;
			this.msg = msg;
			setParameters(params);
		}

		@Override
		public void invoke(Entity entity)
		{
			log.info("Sending {} message to {}", template, entity.getId());
			try
			{
				EntityParam recipient = new EntityParam(entity.getId());
				String name = idsMan.getEntityLabel(recipient);
				name = name == null ? "" : name;
				Map<String, String> params = new HashMap<>();
				params.put(UserNotificationTemplateDef.USER, name);
				notificationProducer.sendNotification(
						recipient, 
						template, 
						params, 
						msg.getDefaultLocaleCode(), 
						null, 
						false);
			} catch (Exception e)
			{
				log.error("Sending notification to entity failed", e);
			}
		}
		
		private void setParameters(String[] parameters)
		{
			template = parameters[0];
		}
	}
}
