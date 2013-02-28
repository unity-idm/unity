/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * @author K. Benedyczak
 */
@Component
public class AttributeSerializer<T> implements JsonSerializer<Attribute<T>>
{
	private final ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] toJson(Attribute<T> src)
	{
		ObjectNode root = mapper.createObjectNode();
		root.put("visibility", src.getVisibility().name());
		ArrayNode values = root.putArray("values");
		AttributeValueSyntax<T> syntax = src.getAttributeSyntax();
		for (T value: src.getValues())
			values.add(syntax.serialize(value));

		try
		{
			return mapper.writeValueAsBytes(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void fromJson(byte[] json, Attribute<T> target)
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
		target.setVisibility(AttributeVisibility.valueOf(main.get("visibility").asText()));
		
		ArrayNode values = main.withArray("values");
		List<T> pValues = new ArrayList<T>(values.size());
		Iterator<JsonNode> it = values.iterator();
		AttributeValueSyntax<T> syntax = target.getAttributeSyntax();
		try
		{
			while(it.hasNext())
				pValues.add(syntax.deserialize(it.next().binaryValue()));
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		target.setValues(pValues);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> getSupportedClass()
	{
		return Attribute.class;
	}

}
