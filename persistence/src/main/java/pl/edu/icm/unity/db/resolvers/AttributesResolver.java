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
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.attributes.AttributeValueSyntaxFactory;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.types.basic.AttributeExt;
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
	private AttributeSyntaxFactoriesRegistry typesRegistry;
	
	@Autowired
	public AttributesResolver(AttributeTypeSerializer atSerializer,
			AttributeSerializer aSerializer,
			AttributeSyntaxFactoriesRegistry typesRegistry)
	{
		super();
		this.atSerializer = atSerializer;
		this.aSerializer = aSerializer;
		this.typesRegistry = typesRegistry;
	}

	public AttributeType resolveAttributeTypeBean(AttributeTypeBean raw) throws IllegalTypeException
	{
		AttributeType at = new AttributeType();
		at.setName(raw.getName());
		AttributeValueSyntaxFactory<?> syntaxFactory = typesRegistry.getByName(raw.getValueSyntaxId());
		@SuppressWarnings("rawtypes")
		AttributeValueSyntax syntax = syntaxFactory.createInstance();
		at.setValueType(syntax);
		atSerializer.fromJson(raw.getContents(), at);
		return at;
	}

	public AttributeTypeBean resolveAttributeType(String attributeName, AttributesMapper mapper) 
			throws IllegalAttributeTypeException
	{
		AttributeTypeBean atBean = mapper.getAttributeType(attributeName);
		if (atBean == null)
			throw new IllegalAttributeTypeException("The attribute type with name " + attributeName + 
					" does not exist");
		return atBean;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public AttributeExt<?> resolveAttributeBean(AttributeBean raw, String groupPath) throws IllegalTypeException
	{
		AttributeExt attr = new AttributeExt();
		attr.setName(raw.getName());
		attr.setGroupPath(groupPath);
		AttributeValueSyntaxFactory<?> syntaxFactory = typesRegistry.getByName(raw.getValueSyntaxId());
		AttributeValueSyntax attributeSyntax = syntaxFactory.createInstance();
		attr.setAttributeSyntax(attributeSyntax);
		attr.setDirect(true);
		aSerializer.fromJson(raw.getValues(), attr);
		return attr;
	}
	
	public List<AttributeExt<?>> convertAttributes(List<AttributeBean> raw, String groupPath) throws IllegalTypeException
	{
		List<AttributeExt<?>> ret = new ArrayList<AttributeExt<?>>(raw.size());
		for (AttributeBean ab: raw)
			ret.add(resolveAttributeBean(ab, groupPath));
		return ret;
	}
}
