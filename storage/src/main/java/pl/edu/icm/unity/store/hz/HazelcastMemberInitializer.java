/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SerializationConfig;
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
	public HazelcastInstance getHazelcastInstance(KryoSerializer globalSerializer)
	{
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
		GlobalSerializerConfig globalSCfg = new GlobalSerializerConfig();
		globalSCfg.setImplementation(globalSerializer);
		serializationConfig.setGlobalSerializerConfig(globalSCfg);
		return Hazelcast.newHazelcastInstance(config);
	}
}
