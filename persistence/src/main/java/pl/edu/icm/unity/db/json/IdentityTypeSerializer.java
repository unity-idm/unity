/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Handles serialization of {@link IdentityType} metadata. The metadata
 * is common for all identity types.
 * @author K. Benedyczak
 */
@Component
public class IdentityTypeSerializer
{
	private ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * @param src
	 * @return Json as byte[] with the src contents.
	 */
	public byte[] toJson(IdentityType src)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("description", src.getDescription());
		main.put("selfModificable", src.isSelfModificable());
		ArrayNode extractedA = main.putArray("extractedAttributes");
		for (Map.Entry<String, String> a: src.getExtractedAttributes().entrySet())
		{
			ObjectNode entry = mapper.createObjectNode();
			entry.put("key", a.getKey());
			entry.put("value", a.getValue());
			extractedA.add(entry);
		}
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
	public void fromJson(byte[] json, IdentityType target)
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
		ArrayNode attrs = main.withArray("extractedAttributes");
		Map<String, String> attrs2 = new HashMap<String, String>();
		for (JsonNode a: attrs)
		{
			attrs2.put(a.get("key").asText(), a.get("value").asText());
		}
		target.setExtractedAttributes(attrs2);
		
		if (main.has("selfModificable"))
			target.setSelfModificable(main.get("selfModificable").asBoolean());
		else
			target.setSelfModificable(false);
	}
}



