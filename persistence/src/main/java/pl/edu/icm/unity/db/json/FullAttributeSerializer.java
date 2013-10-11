/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * Serializes a complete attribute to JSON. The {@link AttributeSerializer} serializes only visibility and values,
 * this implementation also serializes name and group path.
 * @author K. Benedyczak
 */
@Component
public class FullAttributeSerializer
{
	private final ObjectMapper mapper;
	private final DBAttributes dbAttributes;
	
	@Autowired
	public FullAttributeSerializer(ObjectMapper mapper, DBAttributes dbAttributes)
	{
		this.mapper = mapper;
		this.dbAttributes = dbAttributes;
	}

	public <T> ObjectNode toJson(Attribute<T> src)
	{
		ObjectNode root = mapper.createObjectNode();
		root.put("visibility", src.getVisibility().name());
		ArrayNode values = root.putArray("values");
		AttributeValueSyntax<T> syntax = src.getAttributeSyntax();
		for (T value: src.getValues())
			values.add(syntax.serialize(value));
		root.put("name", src.getName());
		root.put("groupPath", src.getGroupPath());
		return root;
	}		

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Attribute<?> fromJson(ObjectNode main, SqlSession sql) throws IllegalAttributeTypeException, IllegalTypeException
	{
		if (main == null)
			return null;
		String name = main.get("name").asText();
		AttributeType type = dbAttributes.getAttributeType(name, sql);
		Attribute ret = new Attribute();
		ret.setAttributeSyntax(type.getValueType());
		ret.setGroupPath(main.get("groupPath").asText());
		ret.setName(name);
		ret.setVisibility(AttributeVisibility.valueOf(main.get("visibility").asText()));
		
		ArrayNode values = main.withArray("values");
		List pValues = new ArrayList(values.size());
		Iterator<JsonNode> it = values.iterator();
		AttributeValueSyntax syntax = ret.getAttributeSyntax();
		try
		{
			while(it.hasNext())
				pValues.add(syntax.deserialize(it.next().binaryValue()));
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
		ret.setValues(pValues);
		return ret;
	}
}
