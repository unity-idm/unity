/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.types.StoredAttribute;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;


/**
 * Serializes {@link Attribute} to/from JSON.
 * @author K. Benedyczak
 */
@Component
public class AttributeJsonSerializer implements JsonSerializerForKryo<StoredAttribute>
{
	private final int attributeSizeLimit;

	@Autowired
	AttributeJsonSerializer(StorageConfiguration storageConfiguration)
	{
		attributeSizeLimit = storageConfiguration.getIntValue(StorageConfiguration.MAX_ATTRIBUTE_SIZE);
	}
	
	@Override
	public StoredAttribute fromJson(ObjectNode main)
	{
		if (main == null)
			return null;
		AttributeExt ret = new AttributeExt(main);
		long entityId = main.get("entityId").asLong();
		return new StoredAttribute(ret, entityId);
	}
	
	@Override
	public ObjectNode toJson(StoredAttribute src)
	{
		ObjectNode root = src.getAttribute().toJson();
		root.put("entityId", src.getEntityId());
		return root;
	}

	@Override
	public Class<StoredAttribute> getClazz()
	{
		return StoredAttribute.class;
	}
	
	@Override
	public void assertSizeLimit(byte [] contents)
	{
		StorageLimits.checkAttributeLimit(contents, attributeSizeLimit);
	}
}
