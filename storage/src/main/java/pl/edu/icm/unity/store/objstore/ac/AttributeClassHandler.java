/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.ac;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;

/**
 * Handler for {@link AttributesClass}
 * @author K. Benedyczak
 */
@Component
public class AttributeClassHandler extends DefaultEntityHandler<AttributesClass>
{
	public static final String ATTRIBUTE_CLASS_OBJECT_TYPE = "attributeClass";
	
	@Autowired
	public AttributeClassHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, ATTRIBUTE_CLASS_OBJECT_TYPE, AttributesClass.class);
	}

	@Override
	public GenericObjectBean toBlob(AttributesClass value)
	{
		try
		{
			byte[] contents = jsonMapper.writeValueAsBytes(AttributeClassMapper.map(value));
			return new GenericObjectBean(value.getName(), contents, supportedType);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize attribute class to JSON", e);
		}
	}

	@Override
	public AttributesClass fromBlob(GenericObjectBean blob)
	{
		try
		{
			return AttributeClassMapper.map(jsonMapper.readValue(blob.getContents(), DBAttributesClass.class));
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize attribute class from JSON", e);
		}
	}
}
