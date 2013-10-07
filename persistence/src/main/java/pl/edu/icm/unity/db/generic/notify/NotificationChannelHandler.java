/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic.notify;

import java.io.IOException;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.db.generic.DefaultEntityHandler;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Handler for {@link NotificationChannel}
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
	public GenericObjectBean toBlob(NotificationChannel value, SqlSession sql)
	{
		try
		{
			ObjectNode root = jsonMapper.createObjectNode();
			root.put("configuration", value.getConfiguration());
			root.put("facilityId", value.getFacilityId());
			root.put("description", value.getDescription());
			byte[] contents = jsonMapper.writeValueAsBytes(root);
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize JSON notification channel state", e);
		}
	}

	@Override
	public NotificationChannel fromBlob(GenericObjectBean blob, SqlSession sql)
	{
		try
		{
			JsonNode root = jsonMapper.readTree(blob.getContents());
			String configuration = root.get("configuration").asText();
			String facility = root.get("facilityId").asText();
			String description = root.get("description").asText();
			return new NotificationChannel(blob.getName(), description, configuration, facility);
		} catch (IOException e)
		{
			throw new InternalException("Can't deserialize JSON notification channel state", e);
		}
	}
}
