/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.notify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.notifications.NotificationChannel;
import pl.edu.icm.unity.store.api.generic.NotificationChannelDB;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.objstore.GenericObjectsDAOImpl;

/**
 * Easy access to {@link NotificationChannel} storage.
 * @author K. Benedyczak
 */
@Component
public class NotificationChannelDBImpl extends GenericObjectsDAOImpl<NotificationChannel> 
			implements NotificationChannelDB
{
	@Autowired
	public NotificationChannelDBImpl(NotificationChannelHandler handler, ObjectStoreDAO dbGeneric)
	{
		super(handler, dbGeneric, NotificationChannel.class, "notification channel");
	}
}
