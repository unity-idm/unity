/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Decorates {@link AttributeTypeSerializer} with methods required to register for Kryo 
 * @author K. Benedyczak
 */
@Component
@Primary
public class AttributeTypeKryoSerializer implements JsonSerializerForKryo<AttributeType>
{
	@Override
	public Class<?> getClazz()
	{
		return AttributeType.class;
	}

	@Override
	public AttributeType fromJson(ObjectNode src)
	{
		return new AttributeType(src);
	}

	@Override
	public ObjectNode toJson(AttributeType src)
	{
		return src.toJson();
	}
}
