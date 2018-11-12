/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.h2.Driver;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.db.DBPropertiesHelper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.StorageConfiguration;

/**
 * Database configuration
 * @author K. Benedyczak
 */
public class RDBMSConfiguration extends PropertiesHelper
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, RDBMSConfiguration.class);
	public enum Dialect {h2, mysql, psql};

	@DocumentationReferencePrefix
	public static final String PREFIX = StorageConfiguration.PREFIX + "engine.rdbms.";
	
	public static final String DBCONFIG_FILE = "mapconfigFile";
	public static final String MAX_POOL_SIZE = "maxConnectionPoolSize";
	public static final String MIN_POOL_SIZE = "minConnectionPoolSize";
	public static final String MAX_IDLE_CONNECTION_TIME = "maxIdleConnectionLifetime";
	private static final String CACHE_MAX_ENTRIES = "cacheMaxEntries";
	private static final String CACHE_TTL = "cacheTTL";
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META;
	static 
	{
		META = DBPropertiesHelper.getMetadata(Driver.class, "jdbc:h2:file:./data/unitydb.bin", 
				Dialect.h2, "");
		META.put(DBCONFIG_FILE, new PropertyMD().setPath().setHidden().
				setDescription("Path of the low level database file with mappings configuration."));
		META.put(MAX_IDLE_CONNECTION_TIME, new PropertyMD("1800").
				setDescription("Time in seconds after which an idle connection "
						+ "is closed (and recreated if needed). Set to 0 to disable. "
						+ "This setting is needed if stale connections become unoperational "
						+ "what unfortunately do happen."));
		META.put(MAX_POOL_SIZE, new PropertyMD("10").
				setDescription("Maximum number of DB connections allowed in pool"));
		META.put(MIN_POOL_SIZE, new PropertyMD("1").
				setDescription("Minimum number of DB connections to be kept in pool"));
		META.put(CACHE_MAX_ENTRIES, new PropertyMD("-1").setDeprecated().
				setDescription("Not used anymore - please remove from configuration"));
		META.put(CACHE_TTL, new PropertyMD("-1").setDeprecated().
				setDescription("Not used anymore - please remove from configuration"));
	}
	
	public RDBMSConfiguration(Properties src) throws ConfigurationException
	{
		super(PREFIX, src, META, log);
	}
	
	protected RDBMSConfiguration(String prefix, Properties src, Map<String, PropertyMD> meta,
			Logger log)
	{
		super(prefix, src, meta, log);
	}

	public Properties getProperties()
	{
		return properties;
	}
}
