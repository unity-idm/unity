/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.store.types.StoredAttribute;


/**
 * Serializes {@link Attribute} to/from JSON.
 * @author K. Benedyczak
 */
@Component
class AttributeJsonSerializer
{
	StoredAttribute fromJson(ObjectNode main)
	{
		if (main == null)
			return null;
		AttributeExt ret;
		try
		{
			ret = AttributeExtMapper
					.map(Constants.MAPPER.treeToValue(main, DBAttributeExt.class));
		} catch (JsonProcessingException | IllegalArgumentException e)
		{
			throw new IllegalStateException("Error parsing attribute from json", e);
		}
		long entityId = main.get("entityId").asLong();
		return new StoredAttribute(ret, entityId);
	}
	
	ObjectNode toJson(StoredAttribute src)
	{
		ObjectNode root = Constants.MAPPER.valueToTree(AttributeExtMapper.map(src.getAttribute()));
		root.put("entityId", src.getEntityId());
		return root;
	}
}
