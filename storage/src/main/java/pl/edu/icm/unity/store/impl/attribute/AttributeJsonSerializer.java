/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;


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
		AttributeExt ret = new AttributeExt(main);
		long entityId = main.get("entityId").asLong();
		return new StoredAttribute(ret, entityId);
	}
	
	ObjectNode toJson(StoredAttribute src)
	{
		ObjectNode root = src.getAttribute().toJson();
		root.put("entityId", src.getEntityId());
		return root;
	}
}
