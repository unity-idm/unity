/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.msgtemplates.UserNotificationTemplateDef;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Sends user notifications invoked through Admin rest API.
 * 
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
@PrototypeComponent
class UserNotificationTriggerer
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER, UserNotificationTriggerer.class);
	
	private NotificationProducer notificationProducer;
	private UnityMessageSource msg;
	private EntityManagement identitiesMan;
	private MessageTemplateManagement msgTemplateMan;
	
	@Autowired
	public UserNotificationTriggerer(NotificationProducer notificationProducer, EntityManagement idsMan, 
			UnityMessageSource msg, MessageTemplateManagement msgTemplateMan)
	{
		this.notificationProducer = notificationProducer;
		this.msg = msg;
		this.identitiesMan = idsMan;
		this.msgTemplateMan = msgTemplateMan;
	}
	
	public void sendNotification(Entity entity, String templateId, Map<String, String> params) throws EngineException
	{
		assertValidUserTemplate(templateId);
		
		LOG.info("Sending {} message to {}, additional params: {}", templateId, entity.getId(), params);
		try
		{
			EntityParam recipient = new EntityParam(entity.getId());
			String name = identitiesMan.getEntityLabel(recipient);
			params.put(UserNotificationTemplateDef.USER, name == null ? "" : name);
			notificationProducer.sendNotification(
					recipient, 
					templateId, 
					params, 
					msg.getDefaultLocaleCode(), 
					null, 
					false);
		} catch (Exception e)
		{
			LOG.error("Sending notification to entity failed", e);
		}
	}

	private void assertValidUserTemplate(String templateId) throws EngineException
	{
		Set<String> userNotifcationTemplateIds = msgTemplateMan.getCompatibleTemplates(
				UserNotificationTemplateDef.NAME).keySet();
		
		if (!userNotifcationTemplateIds.contains(templateId))
		{
			throw new WrongArgumentException("Provided template either does not exist or "
					+ "is not type of " + UserNotificationTemplateDef.NAME + ". Valid "
					+ "templates: " + userNotifcationTemplateIds.toString());
		}
	}
}
