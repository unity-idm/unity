/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.h2.Driver;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import eu.unicore.util.db.DBPropertiesHelper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.StorageConfiguration;

/**
 * Database configuration
 * @author K. Benedyczak
 */
public class RDBMSConfiguration extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, RDBMSConfiguration.class);
	public enum Dialect {h2, mysql, psql};

	@DocumentationReferencePrefix
	public static final String PREFIX = StorageConfiguration.PREFIX + "engine.rdbms.";
	
	public static final String DBCONFIG_FILE = "mapconfigFile";
	private static final String MAX_POOL_SIZE = "maxConnectionPoolSize";
	private static final String MIN_POOL_SIZE = "minConnectionPoolSize";
	private static final String MAX_IDLE_CONNECTION_TIME = "maxIdleConnectionLifetime";
	private static final String CACHE_MAX_ENTRIES = "cacheMaxEntries";
	private static final String CACHE_TTL = "cacheTTL";
	
	public static final String DEFAULT_NETWORK_TIMEOUT_MILLIS = "defaultNetworkTimeout";
	public static final String POOL_MAX_ACTIVE_CONNECTIONS = "poolMaximumActiveConnections";
	public static final String POOL_MAX_IDLE_CONNECTIONS = "poolMaximumIdleConnections";
	public static final String POOL_MAX_CHECKOUT_TIME_MILLIS = "poolMaximumCheckoutTime";
	public static final String POOL_TIME_TO_WAIT_MILLIS = "poolTimeToWait";
	public static final String POOL_MAX_LOCAL_BAD_CONNECTION_TOLERANCE = "poolMaximumLocalBadConnectionTolerance";
	public static final String POOL_PING_CONNECTIONS_NOT_USED_FOR_MILLIS = "poolPingConnectionsNotUsedFor";
	
	public final static DocumentationCategory CONNECTION_CAT = new DocumentationCategory(
			"Low level database connection settings", "2");
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = createMetadata();
	
	
	private static Map<String, PropertyMD> createMetadata()
	{
		Map<String, PropertyMD> meta = DBPropertiesHelper.getMetadata(Driver.class, 
				"jdbc:h2:file:./data/unitydb.bin", Dialect.h2, "");
		meta.put(DBCONFIG_FILE, new PropertyMD().setPath().setHidden().
				setDescription("Path of the low level database file with mappings configuration."));
		meta.put(MAX_IDLE_CONNECTION_TIME, new PropertyMD("1800").setDeprecated().
				setDescription("Time in seconds after which an idle connection "
						+ "is closed (and recreated if needed). Set to 0 to disable. "
						+ "This setting is needed if stale connections become unoperational "
						+ "what unfortunately do happen."));
		meta.put(MAX_POOL_SIZE, new PropertyMD("10").setDeprecated().
				setDescription("Maximum number of DB connections allowed in pool"));
		meta.put(MIN_POOL_SIZE, new PropertyMD("1").setDeprecated().
				setDescription("Minimum number of DB connections to be kept in pool"));
		meta.put(CACHE_MAX_ENTRIES, new PropertyMD("-1").setDeprecated().
				setDescription("Not used anymore - please remove from configuration"));
		meta.put(CACHE_TTL, new PropertyMD("-1").setDeprecated().
				setDescription("Not used anymore - please remove from configuration"));
		
		
		meta.put(DEFAULT_NETWORK_TIMEOUT_MILLIS, new PropertyMD(String.format("%d", TimeUnit.MINUTES.toMillis(30)))
				.setCategory(CONNECTION_CAT)
				.setDescription("The default network timeout value in milliseconds to wait for the database "
						+ "operation to complete. When set to zero then "
						+ "such configuration is interpreted as an infinite timeout.")
		);
		meta.put(POOL_MAX_ACTIVE_CONNECTIONS, new PropertyMD("20").setCategory(CONNECTION_CAT)
				.setDescription("The number of active (i.e. in use) connections that can exist at any "
						+ "given time.")
		);
		meta.put(POOL_MAX_IDLE_CONNECTIONS, new PropertyMD("10").setCategory(CONNECTION_CAT)
				.setDescription("The number of idle connections that can exist at any given time.")
		);
		meta.put(POOL_MAX_CHECKOUT_TIME_MILLIS, new PropertyMD("200000").setCategory(CONNECTION_CAT)
				.setDescription("This is the amount of time in milliseconds that a Connection can be \"checked out\" "
						+ "of the pool before it will be forcefully returned.")
		);
		meta.put(POOL_TIME_TO_WAIT_MILLIS, new PropertyMD("500").setCategory(CONNECTION_CAT)
				.setDescription("The time in milliseconds to wait before retrying to get a connection from the pool.")
		);
		meta.put(POOL_MAX_LOCAL_BAD_CONNECTION_TOLERANCE, new PropertyMD("3").setCategory(CONNECTION_CAT)
				.setDescription(String.format("This is a low level setting about tolerance of bad connections got for "
						+ "any thread. If a thread got a bad connection, it may still have another chance to re-attempt "
						+ "to get another connection which is valid. But the retrying times should not more than the "
						+ "sum of %s and %s.", POOL_MAX_IDLE_CONNECTIONS, POOL_MAX_LOCAL_BAD_CONNECTION_TOLERANCE))
		);
		meta.put(POOL_PING_CONNECTIONS_NOT_USED_FOR_MILLIS, new PropertyMD("600000").setCategory(CONNECTION_CAT)
				.setDescription("Frequency in milliseconds of a ping query on an idle connection in a pool, "
						+ "used to keep the conection active. Can be set to match the typical timeout for a "
						+ "database connection, to avoid unnecessary pings.")
		);
		return meta;
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
