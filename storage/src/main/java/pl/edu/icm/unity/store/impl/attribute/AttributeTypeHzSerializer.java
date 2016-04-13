/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import pl.edu.icm.unity.base.attributes.AttributeTypeSerializer;
import pl.edu.icm.unity.store.hz.SerializerProvider;
import pl.edu.icm.unity.types.basic.AttributeType;


/**
 * Serialization of {@link AttributeType} for hazelcast
 * @author K. Benedyczak
 */
@Component
public class AttributeTypeHzSerializer implements SerializerProvider<AttributeType> 
{
	@Autowired
	private AttributeTypeSerializer atSerializer;
	
	@Override
	public void write(ObjectDataOutput out, AttributeType object) throws IOException
	{
		out.writeByteArray(atSerializer.toJsonFull(object));
	}

	@Override
	public AttributeType read(ObjectDataInput in) throws IOException
	{
		return atSerializer.fromJsonFull(in.readByteArray());
	}

	@Override
	public int getTypeId()
	{
		return 2;
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public Class<AttributeType> getTypeClass()
	{
		return AttributeType.class;
	}
}
