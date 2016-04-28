/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attributetype;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attributes.AttributeTypeSerializer;
import pl.edu.icm.unity.store.hz.JsonSerializerForKryo;
import pl.edu.icm.unity.types.basic.AttributeType;

/**
 * Decorates {@link AttributeTypeSerializer} with methods required to register for Kryo 
 * @author K. Benedyczak
 */
@Component
@Primary
public class AttributeTypeKryoSerializer extends AttributeTypeSerializer 
	implements JsonSerializerForKryo<AttributeType>
{
	@Override
	public Class<?> getClazz()
	{
		return AttributeType.class;
	}
}
