/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.notify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.store.api.generic.NotificationChannelDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Handles import/export of {@link NotificationChannel}.
 * @author K. Benedyczak
 */
@Component
public class NotificationChannelIE extends GenericObjectIEBase<NotificationChannel>
{
	@Autowired
	public NotificationChannelIE(NotificationChannelDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, NotificationChannel.class, 106, "notificationChannels");
	}
}



