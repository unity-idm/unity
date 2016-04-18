/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

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
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = new HashMap<>();
	static 
	{
		META.putAll(RDBMSConfiguration.META);
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
