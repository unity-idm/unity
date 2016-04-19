/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.hazelcast.query.impl.AttributeType;

import pl.edu.icm.unity.base.attributes.AttributeTypeSerializer;
import pl.edu.icm.unity.store.impl.identitytype.IdentityTypeJsonSerializer;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSEventsBatch;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Creates {@link Kryo} engine, configured with all required type serializers.
 * @author K. Benedyczak
 */
@Configuration
public class KryoPoolFactory
{
	@Autowired
	private AttributeTypeSerializer attributeTypeJS;
	@Autowired
	private IdentityTypeJsonSerializer identityTypeJS;
	
	
	public Kryo getInstance()
	{
		Kryo kryo = new Kryo();
		kryo.register(AttributeType.class, new KryoJsonSerializer<>(attributeTypeJS));
		kryo.register(IdentityType.class, new KryoJsonSerializer<>(identityTypeJS));
		kryo.register(RDBMSEventsBatch.class);
		kryo.register(RDBMSMutationEvent.class);
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
