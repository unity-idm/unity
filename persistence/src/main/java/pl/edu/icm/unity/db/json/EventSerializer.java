/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.events.Event;

/**
 * Handles serialization of {@link Event}s.
 * @author K. Benedyczak
 */
@Component
public class EventSerializer
{
	@Autowired
	private ObjectMapper mapper;
	
	/**
	 * @param src
	 * @return Json as byte[] with the src contents.
	 */
	public String toJson(Event src)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("category", src.getCategory());
		main.put("contents", src.getContents());
		main.put("invokerEntity", src.getInvokerEntity());
		main.put("timestamp", src.getTimestamp().getTime());
		try
		{
			return mapper.writeValueAsString(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
	
	/**
	 * Fills target with JSON contents, checking it for correctness
	 * @param json
	 * @param target
	 */
	public Event fromJson(String json)
	{
		ObjectNode main;
		try
		{
			main = mapper.readValue(json, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

		String category = main.get("category").asText();
		String contents = main.get("contents").asText();
		long invokerEntity = main.get("invokerEntity").asLong();
		long ts = main.get("timestamp").asLong();
		return new Event(category, invokerEntity, new Date(ts), contents);
	}
}
