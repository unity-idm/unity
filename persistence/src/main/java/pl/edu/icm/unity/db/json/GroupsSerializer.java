/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.Group;

/**
 * Handles serialization of Groups metadata.
 * @author K. Benedyczak
 */
@Component
public class GroupsSerializer implements JsonSerializer<Group>
{
	private ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * @param src
	 * @return Json as byte[] with the src contents.
	 */
	public byte[] toJson(Group src)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("description", src.getDescription());
		//TODO handle attribute statements
		try
		{
			return mapper.writeValueAsBytes(main);
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
	public void fromJson(byte[] json, Group target)
	{
		if (json == null)
			return;
		ObjectNode main;
		try
		{
			main = mapper.readValue(json, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

		target.setDescription(main.get("description").asText());
		//TODO handle attribute statements
	}

	@Override
	public Class<Group> getSupportedClass()
	{
		return Group.class;
	}
}
