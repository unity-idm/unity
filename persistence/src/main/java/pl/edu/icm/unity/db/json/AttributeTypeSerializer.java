/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.db.AttributeValueTypesRegistry;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.AttributeType;
import pl.edu.icm.unity.types.AttributeValueSyntax;
import pl.edu.icm.unity.types.AttributeVisibility;

/**
 * @author K. Benedyczak
 */
@Component
public class AttributeTypeSerializer implements JsonSerializer<AttributeType>
{
	private final ObjectMapper mapper = new ObjectMapper();
	private AttributeValueTypesRegistry typesRegistry;
	
	@Autowired
	public AttributeTypeSerializer(AttributeValueTypesRegistry typesRegistry)
	{
		this.typesRegistry = typesRegistry;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte[] toJson(AttributeType src)
	{
		ObjectNode root = mapper.createObjectNode();
		root.put("description", src.getDescription());
		root.put("flags", src.getFlags());
		root.put("maxElements", src.getMaxElements());
		root.put("minElements", src.getMinElements());
		root.put("selfModificable", src.isSelfModificable());
		root.put("visibility", src.getVisibility().name());
		root.put("valueType", src.getValueType().getValueSyntaxId());
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
	public void fromJson(byte[] json, AttributeType target)
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
		target.setFlags(main.get("flags").asInt());
		target.setMaxElements(main.get("maxElements").asInt());
		target.setMinElements(main.get("minElements").asInt());
		target.setSelfModificable(main.get("selfModificable").asBoolean());
		target.setVisibility(AttributeVisibility.valueOf(main.get("visibility").asText()));
		String syntaxId = main.get("valueType").asText();
		@SuppressWarnings("rawtypes")
		AttributeValueSyntax syntax = typesRegistry.getByName(syntaxId);
		target.setValueType(syntax);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<AttributeType> getSupportedClass()
	{
		return AttributeType.class;
	}

}
