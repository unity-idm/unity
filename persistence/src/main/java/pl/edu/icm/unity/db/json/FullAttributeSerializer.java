/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.DBAttributes;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Serializes a complete attribute to JSON. The {@link AttributeSerializer} serializes only visibility and values,
 * this implementation also serializes name and group path.
 * @author K. Benedyczak
 */
@Component
public class FullAttributeSerializer extends AbstractAttributeSerializer
{
	private final DBAttributes dbAttributes;
	
	@Autowired
	public FullAttributeSerializer(DBAttributes dbAttributes)
	{
		this.dbAttributes = dbAttributes;
	}

	public <T> ObjectNode toJson(Attribute<T> src)
	{
		ObjectNode root = toJsonBase(src);
		root.put("name", src.getName());
		root.put("groupPath", src.getGroupPath());
		return root;
	}		

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Attribute<?> fromJson(ObjectNode main, SqlSession sql) 
			throws IllegalAttributeTypeException, IllegalTypeException
	{
		if (main == null)
			return null;
		String name = main.get("name").asText();
		AttributeType type = dbAttributes.getAttributeType(name, sql);
		Attribute ret = new Attribute();
		ret.setAttributeSyntax(type.getValueType());
		ret.setGroupPath(main.get("groupPath").asText());
		ret.setName(name);

		fromJsonBase(main, ret);
		return ret;
	}
}
