/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.h2.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.db.DBPropertiesHelper;

/**
 * Database configuration
 * @author K. Benedyczak
 */
@Component
public class DBConfiguration extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, DBConfiguration.class);
	public enum Dialect {h2, mysql, psql};

	public static final String PREFIX = UnityServerConfiguration.BASE_PREFIX+DBPropertiesHelper.PREFIX;
	
	public static final String DBCONFIG_FILE = "mapconfigFile";
	
	public static final Map<String, PropertyMD> META;
	static 
	{
		META = DBPropertiesHelper.getMetadata(Driver.class, "jdbc:h2:file:data/unitydb.bin", 
				Dialect.h2, "");
		META.put(DBCONFIG_FILE, new PropertyMD().setPath().setHidden().
				setDescription("Path of the low level database file with mappings configuration."));
	}
	
	@Autowired
	public DBConfiguration(UnityServerConfiguration main) throws ConfigurationException
	{
		super(PREFIX, main.getProperties(), META, log);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
