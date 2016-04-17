/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import pl.edu.icm.unity.store.rdbmsflush.RDBMSEventsBatch;

/**
 * Kryo based default global serializer for Hazelcast
 * @author K. Benedyczak
 */
@Component
public class KryoSerializer implements StreamSerializer<Object>
{
	@Autowired
	private KryoPool kryoP;	
	
	@Override
	public void write(ObjectDataOutput out, Object batch) throws IOException
	{
		Output output = new Output((OutputStream) out);
		Kryo kryo = kryoP.borrow();
		try
		{
			kryo.writeObject(output, batch);
		} finally
		{
			kryoP.release(kryo);
		}
		
		output.flush();
	}

	@Override
	public Object read(ObjectDataInput odin) throws IOException
	{
		InputStream in = (InputStream) odin;
		Input input = new Input(in);
		Kryo kryo = kryoP.borrow();
		try
		{
		        return kryo.readObject(input, RDBMSEventsBatch.class);
		} finally
		{
			kryoP.release(kryo);
		}
	}
	
	@Override
	public int getTypeId()
	{
		return 1000;
	}

	@Override
	public void destroy()
	{
	}
}
