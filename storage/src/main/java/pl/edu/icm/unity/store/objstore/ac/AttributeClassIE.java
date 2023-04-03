/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.ac;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.api.generic.AttributeClassDB;
import pl.edu.icm.unity.store.objstore.GenericObjectIEBase;
import pl.edu.icm.unity.types.basic.AttributesClass;

/**
 * Handles import/export of {@link AttributesClass}.
 * @author K. Benedyczak
 */
@Component
public class AttributeClassIE extends GenericObjectIEBase<AttributesClass>
{
	@Autowired
	public AttributeClassIE(AttributeClassDB dao, ObjectMapper jsonMapper)
	{
		super(dao, jsonMapper, 100, AttributeClassHandler.ATTRIBUTE_CLASS_OBJECT_TYPE);
	}

	@Override
	protected AttributesClass convert(ObjectNode src)
	{
		return AttributeClassMapper.map(jsonMapper.convertValue(src, DBAttributesClass.class));
	}

	@Override
	protected ObjectNode convert(AttributesClass src)
	{
		return jsonMapper.convertValue(AttributeClassMapper.map(src), ObjectNode.class);
	}
}



