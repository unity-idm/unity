/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.config.GlobalSerializerConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SerializationConfig;
import com.hazelcast.config.SerializerConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StorageEngine;

/**
 * Initializes the current instance as Hazelcast cluster member. 
 * @author K. Benedyczak
 */
@Configuration
public class HazelcastMemberInitializer
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB,
			HazelcastMemberInitializer.class);
	
	@Autowired
	@Bean
	public HazelcastInstance getHazelcastInstance(KryoSerializer globalSerializer, StorageConfiguration systemCfg,
			List<MapConfigProvider> mapConfigProviders)
	{
		Config config;
		if (systemCfg.getEnumValue(StorageConfiguration.ENGINE, StorageEngine.class) != StorageEngine.hz)
		{
			log.info("Hazelcast subsystem is not enabled, loading minimal, not clustered vaersion.");
			HzConfiguration def = new HzConfiguration(new Properties());
			config = createConfig(def);
		} else
		{
			HzConfiguration cfg = systemCfg.getEngineConfig();
			config = cfg.isSet(HzConfiguration.EXTERNAL_HZ_CONFIG) ? 
				loadConfigFromFile(cfg) : createConfig(cfg);
		}
		
		config.setProperty("hazelcast.logging.type", "log4j");
		SerializationConfig serializationConfig = config.getSerializationConfig();
		GlobalSerializerConfig globalSCfg = new GlobalSerializerConfig();
		globalSCfg.setImplementation(globalSerializer);
		serializationConfig.setGlobalSerializerConfig(globalSCfg);
		
		for (MapConfigProvider mcp: mapConfigProviders)
			config.addMapConfig(mcp.getMapConfig());
		
		//Workaround for Hazelcast issue https://github.com/hazelcast/hazelcast/issues/6287
		SerializerConfig serializerConfigMap = new SerializerConfig();
		serializerConfigMap.setTypeClass(Map.class);
		serializerConfigMap.setImplementation(globalSerializer);
		serializationConfig.addSerializerConfig(serializerConfigMap);
		
		return Hazelcast.getOrCreateHazelcastInstance(config);
	}
	
	private Config loadConfigFromFile(HzConfiguration cfg)
	{
		File file = cfg.getFileValue(HzConfiguration.EXTERNAL_HZ_CONFIG, false);
		log.info("Loading Hazelcast subsystem with configuration from file " + file);
		try
		{
			return new FileSystemXmlConfig(file);
		} catch (Exception e)
		{
			throw new ConfigurationException("Error loading external Hazelcast configuration from " 
					+ file, e);
		}
	}
	
	private Config createConfig(HzConfiguration cfg)
	{
		log.info("Loading Hazelcast subsystem");
		Config config = new Config();
		config.setInstanceName(cfg.getValue(HzConfiguration.INSTANCE_NAME));
		NetworkConfig networkConfig = config.getNetworkConfig();
		networkConfig.setPort(cfg.getIntValue(HzConfiguration.INTERFACE_PORT))
			.setPortAutoIncrement(false)
			.setReuseAddress(true);
		networkConfig.getInterfaces()
			.setEnabled(true)
			.addInterface(cfg.getValue(HzConfiguration.INTERFACE_IP));
		JoinConfig join = networkConfig.getJoin();
		join.getMulticastConfig().setEnabled(false);
		
		List<String> members = cfg.getListOfValues(HzConfiguration.MEMBERS);
		
		TcpIpConfig tcpIpConfig = join.getTcpIpConfig();
		for (String member: members)
			tcpIpConfig.addMember(member);
		tcpIpConfig.setEnabled(true);
		
		return config;
	}
}
