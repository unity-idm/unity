/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.msgtemplate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.notifications.MessageTemplate;

/**
 * Easy to use interface to {@link MessageTemplate} storage.
 *  
 * @author P. Piernik
 */
@Component
public class MessageTemplateDB extends GenericObjectsDB<MessageTemplate>
{

	@Autowired
	public MessageTemplateDB(MessageTemplateHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, MessageTemplate.class, "message template");
	}
}

