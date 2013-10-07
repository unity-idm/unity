/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.notify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.generic.DependencyNotificationManager;
import pl.edu.icm.unity.db.generic.GenericObjectsDB;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Easy access to {@link NotificationChannel} storage.
 * @author K. Benedyczak
 */
@Component
public class NotificationChannelDB extends GenericObjectsDB<NotificationChannel>
{
	@Autowired
	public NotificationChannelDB(NotificationChannelHandler handler,
			DBGeneric dbGeneric, DependencyNotificationManager notificationManager)
	{
		super(handler, dbGeneric, notificationManager, NotificationChannel.class,
				"notification channel");
	}
}
