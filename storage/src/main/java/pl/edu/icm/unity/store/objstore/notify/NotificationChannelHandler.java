/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.notify;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.notifications.NotificationChannel;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

/**
 * Handler for {@link NotificationChannel}
 * 
 * @author K. Benedyczak
 */
@Component
public class NotificationChannelHandler extends DefaultEntityHandler<NotificationChannel>
{
	public static final String NOTIFICATION_CHANNEL_ID = "notificationChannel";

	@Autowired
	public NotificationChannelHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, NOTIFICATION_CHANNEL_ID, NotificationChannel.class);
	}

	@Override
	public GenericObjectBean toBlob(NotificationChannel value)
	{
		try
		{
			return new GenericObjectBean(value.getName(),
					jsonMapper.writeValueAsBytes(NotifiacationChannelMapper.map(value)), supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize notification channel to JSON", e);
		}
	}

	@Override
	public NotificationChannel fromBlob(GenericObjectBean blob)
	{
		try
		{
			return NotifiacationChannelMapper
					.map(jsonMapper.readValue(blob.getContents(), DBNotificationChannel.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize notification channel from JSON", e);
		}
	}
}
