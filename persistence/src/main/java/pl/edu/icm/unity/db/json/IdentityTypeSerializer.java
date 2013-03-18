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
		//ArrayNode extractedA = main.putArray("extractedAttributes");
		//TODO - store ids of attributes
//		for (String a: src.getExtractedAttributes())
//			extractedA.add(a);
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
//		ArrayNode attrs = main.withArray("extractedAttributes");
//		List<String> attrs2 = new ArrayList<String>();
		//TODO checking if attribtues exist
//		for (JsonNode a: attrs)
//		{
//			attrs2.add(a.asText());
//		}
//		target.setExtractedAttributes(attrs2);
	}
}
