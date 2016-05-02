/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pl.edu.icm.unity.store.rdbmsflush.RDBMSEventsBatch;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSMutationEvent;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

/**
 * Creates {@link Kryo} engine, configured with all required type serializers.
 * @author K. Benedyczak
 */
@Configuration
public class KryoPoolFactory
{
	@Autowired
	private List<JsonSerializerForKryo<?>> jsonSerializers;
	
	public Kryo getInstance()
	{
		Kryo kryo = new Kryo();
		for (JsonSerializerForKryo<?> ser: jsonSerializers)
			kryo.register(ser.getClazz(), new KryoJsonSerializer<>(ser));
		kryo.register(RDBMSEventsBatch.class);
		kryo.register(RDBMSMutationEvent.class);
		kryo.register(Map.class);
		return kryo;
	}
	
	@Bean
	public KryoPool getKryoPool()
	{
		KryoFactory factory = new KryoFactory()
		{
			@Override
			public Kryo create()
			{
				return getInstance();
			}
		};
		
		return new KryoPool.Builder(factory).softReferences().build();
	}
}
