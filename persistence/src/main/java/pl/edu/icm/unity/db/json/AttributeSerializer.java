/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.util.Date;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.AttributeExt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author K. Benedyczak
 */
@Component
public class AttributeSerializer extends AbstractAttributeSerializer
{
	private final ObjectMapper mapper = new ObjectMapper();
	
	public <T> byte[] toJson(AttributeExt<T> src)
	{
		ObjectNode root = toJsonBase(src);

		if (src.getCreationTs() != null)
			root.put("creationTs", src.getCreationTs().getTime());
		if (src.getUpdateTs() != null)
			root.put("updateTs", src.getUpdateTs().getTime());
		try
		{
			return mapper.writeValueAsBytes(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}

	public Long getCreationTs(byte[] json)
	{
		ObjectNode main;
		try
		{
			main = mapper.readValue(json, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		if (main.has("creationTs"))
			return main.get("creationTs").asLong();
		return null;
	}

	public <T> void fromJson(byte[] json, AttributeExt<T> target)
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
		
		fromJsonBase(main, target);
		
		if (main.has("creationTs"))
			target.setCreationTs(new Date(main.get("creationTs").asLong()));
		if (main.has("updateTs"))
			target.setUpdateTs(new Date(main.get("updateTs").asLong()));
	}
}
