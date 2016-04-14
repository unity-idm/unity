/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attributes.AttributeTypeSerializer;
import pl.edu.icm.unity.store.hz.AbstractSerializer;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * Serialization of {@link AttributeType} for hazelcast
 * @author K. Benedyczak
 */
@Component
public class AttributeTypeHzSerializer extends AbstractSerializer<AttributeType> 
{
	@Autowired
	public AttributeTypeHzSerializer(AttributeTypeSerializer serializer)
	{
		super(2, AttributeType.class, serializer);
	}
}
