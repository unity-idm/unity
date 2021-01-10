/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.rdbms.RDBMSConfiguration;

/**
 * Hazelcast storage configuration. Extends {@link RDBMSConfiguration} as the RDBMS is regularry used
 * with this engine to persist data in background.
 * @author K. Benedyczak
 */
public class HzConfiguration extends RDBMSConfiguration
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, HzConfiguration.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = StorageConfiguration.PREFIX + "engine.hz.";
	
	public static final String EXTERNAL_HZ_CONFIG = "externalHazelcastConfigFile";
	public static final String INSTANCE_NAME = "instanceName";
	public static final String MEMBERS = "members.";
	public static final String INTERFACE_IP = "listenAddress";
	public static final String INTERFACE_PORT = "port";
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = new HashMap<>();
	static 
	{
		META.putAll(RDBMSConfiguration.META);
		META.put(EXTERNAL_HZ_CONFIG, new PropertyMD().setPath().
				setDescription("FOR ADVANCED USE only. "
						+ "If set then all other options are ignored. Configuration is"
						+ "loaded from the given path, and must be specified "
						+ "in Hazelcast XML configuration format. Note that after loading Unity "
						+ "performs couple of additional changes in configuration, "
						+ "especially related to serialization configuration, "
						+ "so do not set it manually."));
		META.put(INSTANCE_NAME, new PropertyMD("unity-1").
				setDescription("Name of the cluster member. Must be unique in the cluster."));
		META.put(MEMBERS, new PropertyMD().setList(false).
				setDescription("List with IPs of cluster members. "
						+ "For safety it is strongly adviced to provide all cluster members."));
		META.put(INTERFACE_IP, new PropertyMD("127.0.0.1").
				setDescription("Listen IP of the internal cluster interface. "
						+ "Should not be accessible from outside."));
		META.put(INTERFACE_PORT, new PropertyMD("5701").
				setDescription("Port used for the internal cluster communication"));
		
	}
	
	public HzConfiguration(Properties src) throws ConfigurationException
	{
		super(PREFIX, src, META, log);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
