/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.resolvers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.AttributeSerializer;
import pl.edu.icm.unity.db.json.AttributeTypeSerializer;
import pl.edu.icm.unity.db.mapper.AttributesMapper;
import pl.edu.icm.unity.db.model.AttributeBean;
import pl.edu.icm.unity.db.model.AttributeTypeBean;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.server.registries.AttributeValueTypesRegistry;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;

/**
 * Resolvers of attributes and attribute types.
 * @author K. Benedyczak
 */
@Component
public class AttributesResolver
{
	private AttributeTypeSerializer atSerializer;
	private AttributeSerializer aSerializer;
	private AttributeValueTypesRegistry typesRegistry;
	
	@Autowired
	public AttributesResolver(AttributeTypeSerializer atSerializer,
			AttributeSerializer aSerializer,
			AttributeValueTypesRegistry typesRegistry)
	{
		super();
		this.atSerializer = atSerializer;
		this.aSerializer = aSerializer;
		this.typesRegistry = typesRegistry;
	}

	public AttributeType resolveAttributeTypeBean(AttributeTypeBean raw)
	{
		AttributeType at = new AttributeType();
		at.setName(raw.getName());
		@SuppressWarnings("rawtypes")
		AttributeValueSyntax syntax = typesRegistry.getByName(raw.getValueSyntaxId());
		at.setValueType(syntax);
		atSerializer.fromJson(raw.getContents(), at);
		return at;
	}

	public AttributeTypeBean resolveAttributeType(String attributeName, AttributesMapper mapper)
	{
		AttributeTypeBean atBean = mapper.getAttributeType(attributeName);
		if (atBean == null)
			throw new IllegalAttributeTypeException("The attribute type with name " + attributeName + 
					" does not exist");
		return atBean;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Attribute<?> resolveAttributeBean(AttributeBean raw, String groupPath)
	{
		Attribute attr = new Attribute();
		attr.setName(raw.getName());
		attr.setGroupPath(groupPath);
		AttributeValueSyntax attributeSyntax = typesRegistry.getByName(raw.getValueSyntaxId());
		attr.setAttributeSyntax(attributeSyntax);
		aSerializer.fromJson(raw.getValues(), attr);
		return attr;
	}
	
	public List<Attribute<?>> convertAttributes(List<AttributeBean> raw, String groupPath)
	{
		List<Attribute<?>> ret = new ArrayList<Attribute<?>>(raw.size());
		for (AttributeBean ab: raw)
			ret.add(resolveAttributeBean(ab, groupPath));
		return ret;
	}
}
