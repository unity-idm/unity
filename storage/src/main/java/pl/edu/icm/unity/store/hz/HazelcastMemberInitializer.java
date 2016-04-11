/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * Initializes the current instance as Hazelcast cluster member. 
 * TODO make this fully configurable
 * @author K. Benedyczak
 */
@Configuration
public class HazelcastMemberInitializer
{
	@Autowired
	@Bean
	public HazelcastInstance getHazelcastInstance(Optional<List<SerializerProvider<?>>> serializers)
	{
		List<SerializerProvider<?>> sProviders = serializers.orElseGet(ArrayList::new);
		
		Config config = new Config();
		config.setInstanceName("unity-1");
		config.setProperty("hazelcast.logging.type", "log4j");
		
		NetworkConfig networkConfig = config.getNetworkConfig();
		networkConfig.setPort(5701)
			.setPortAutoIncrement(false)
			.setReuseAddress(true);
		networkConfig.getInterfaces()
			.setEnabled(true)
			.addInterface("127.0.0.1");
		JoinConfig join = networkConfig.getJoin();
		join.getMulticastConfig().setEnabled(false);
		join.getTcpIpConfig()
			.addMember("localhost")
		        .setEnabled(true);
		
		SerializationConfig serializationConfig = config.getSerializationConfig();
		for (SerializerProvider<?> provider: sProviders)
		{
			SerializerConfig cfg = new SerializerConfig();
			cfg.setImplementation(provider);
			cfg.setTypeClass(provider.getTypeClass());
			serializationConfig.addSerializerConfig(cfg);
		}
		return Hazelcast.newHazelcastInstance(config);
	}
}
