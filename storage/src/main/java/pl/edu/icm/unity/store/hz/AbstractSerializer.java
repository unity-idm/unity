/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.io.IOException;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.JsonSerializer;

/**
 * Base class for Hazelcast serializers of objects.
 * @author K. Benedyczak
 */
public abstract class AbstractSerializer<T> implements SerializerProvider<T>
{
	private int typeId;
	private Class<T> typeClass;
	private JsonSerializer<T> serializer;
	
	public AbstractSerializer(int typeId, Class<T> typeClass, JsonSerializer<T> serializer)
	{
		this.typeId = typeId;
		this.typeClass = typeClass;
		this.serializer = serializer;
	}

	@Override
	public void write(ObjectDataOutput out, T object) throws IOException
	{
		out.writeByteArray(JsonUtil.serialize2Bytes(serializer.toJson(object)));
	}

	@Override
	public T read(ObjectDataInput in) throws IOException
	{
		return serializer.fromJson(JsonUtil.parse(in.readByteArray()));
	}

	@Override
	public int getTypeId()
	{
		return typeId;
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public Class<T> getTypeClass()
	{
		return typeClass;
	}
}
