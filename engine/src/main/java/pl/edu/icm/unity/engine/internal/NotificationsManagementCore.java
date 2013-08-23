/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.internal;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.engine.NotificationsManagementImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.notifications.NotificationFacility;

/**
 * Code common to notifications management  {@link NotificationsManagementImpl} and {@link NotificationsProducer}. 
 * @author K. Benedyczak
 */
@Component
public class NotificationsManagementCore
{
	private Map<String, NotificationFacility> facilities;
	private ObjectMapper mapper;

	@Autowired
	public NotificationsManagementCore(Set<NotificationFacility> facilities,
			ObjectMapper mapper)
	{
		this.facilities = new HashMap<>(facilities.size());
		for (NotificationFacility facility: facilities)
			this.facilities.put(facility.getName(), facility);
		this.mapper = mapper;
	}

	public Set<String> getNotificationFacilities() throws EngineException
	{
		return new HashSet<>(facilities.keySet());
	}

	public NotificationFacility getNotificationFacility(String name) throws EngineException
	{
		return facilities.get(name);
	}
	
	public byte[] serializeChannel(String configuration)
	{
		try
		{
			ObjectNode root = mapper.createObjectNode();
			root.put("configuration", configuration);
			return mapper.writeValueAsBytes(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize JSON notification channel state", e);
		}
	}
	
	public String deserializeChannel(byte[] serializedState)
	{
		try
		{
			JsonNode root = mapper.readTree(serializedState);
			return root.get("configuration").asText();
		} catch (IOException e)
		{
			throw new InternalException("Can't deserialize JSON notification channel state", e);
		}
	}
}
