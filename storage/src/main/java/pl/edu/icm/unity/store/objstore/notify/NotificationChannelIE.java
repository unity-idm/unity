/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.notify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.NotificationChannelDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Handles import/export of {@link NotificationChannel}.
 * 
 * @author K. Benedyczak
 */
@Component
public class NotificationChannelIE extends GenericObjectIEBase<NotificationChannel>
{
	@Autowired
	public NotificationChannelIE(NotificationChannelDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 106, NotificationChannelHandler.NOTIFICATION_CHANNEL_ID);
	}

	@Override
	protected NotificationChannel convert(ObjectNode src)
	{
		return NotifiacationChannelMapper.map(jsonMapper.convertValue(src, DBNotificationChannel.class));
	}

	@Override
	protected ObjectNode convert(NotificationChannel src)
	{
		return jsonMapper.convertValue(NotifiacationChannelMapper.map(src), ObjectNode.class);
	}
}
