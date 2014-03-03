/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Handles serialization of {@link IdentityParam} metadata.
 * @author K. Benedyczak
 */
@Component
public class IdentitySerializer
{
	private ObjectMapper mapper = Constants.MAPPER;
	
	/**
	 * @param src
	 * @return Json as byte[] with the src contents.
	 */
	public byte[] toJson(IdentityParam src)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("local", src.isLocal());
		main.put("value", src.getValue());
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
	public void fromJson(byte[] json, IdentityParam target)
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

		target.setLocal(main.get("local").asBoolean());
		target.setValue(main.get("value").asText());
	}
}
