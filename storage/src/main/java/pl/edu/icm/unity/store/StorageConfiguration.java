/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.api.StoragePropertiesSource;

/**
 * Unity storage configuration
 * @author K. Benedyczak
 */
@Component
public class StorageConfiguration extends PropertiesHelper
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, StorageConfiguration.class);

	@DocumentationReferencePrefix
	public static final String PREFIX = "unityServer.storage.";
	
	public static final String ENGINE = "engine";
	public static final String IGNORE_ALTERNATIVE_DB_CONFIG = "ignoreAlternativeDbConfig";
	public static final String MAX_ATTRIBUTE_SIZE = "attributeSizeLimit";
	
	public static final String WIPE_DB_AT_STARTUP = "wipeDbAtStartup";
	
	/**
	 * System property: if set it is providing an alternative path to a file with DB configuration.
	 * It can also accept predefined values which load default storage configurations for tests - see 
	 * test resources of this class for available configs. 
	 */
	public static final String ALTERNATIVE_DB_CONFIG = "unityDbConfig";
	
	@DocumentationReferenceMeta
	public static final Map<String, PropertyMD> META = new HashMap<>();

	private PropertiesHelper engineConfig;
	
	static 
	{
		META.put(ENGINE, new PropertyMD(StorageEngine.rdbms).setCanHaveSubkeys().
				setDescription("Storage engine to be used."));
		META.put(WIPE_DB_AT_STARTUP, new PropertyMD("false").setHidden().
				setDescription("For testing: if set to true then DB will be fully "
						+ "cleared at server startup"));
		META.put(MAX_ATTRIBUTE_SIZE, new PropertyMD("256000").
				setDescription("Controls maximum allowed size of an individual attribute as serialized for storage,"
						+ " including metadata and all values. This limit is not affecting database"
						+ " performance a lot, but allowing for very big values makes Unity more memory hungry."));
		META.put(IGNORE_ALTERNATIVE_DB_CONFIG, new PropertyMD("false").setHidden().
				setDescription("For unity tests: if set in the main configuration then the system "
						+ "property with alternative DB config is ignored. It is useful "
						+ "when test case works only with specific DB configuration and"
						+ "manual, general purpose config has no sense."));
		
	}
	
	@Autowired
	public StorageConfiguration(Map<String, StorageConfigurationFactory> storageConfFactories, 
			StoragePropertiesSource src) throws ConfigurationException
	{
		super(PREFIX, loadEngineSpecificProperties(src.getProperties()), META, log);
		
		StorageEngine engine = getEnumValue(ENGINE, StorageEngine.class);
		String beanName = StorageConfigurationFactory.BEAN_PFX + engine;
		StorageConfigurationFactory cfgFactory = storageConfFactories.get(beanName);
		if (cfgFactory == null)
			throw new InternalException("There is no configuration factory with bean name " + beanName + 
					", this is bug");
		
		engineConfig = cfgFactory.getInstance(properties);
	}
	
	public StorageEngine getEngine()
	{
		return getEnumValue(StorageConfiguration.ENGINE, StorageEngine.class);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends PropertiesHelper> T getEngineConfig()
	{
		return (T) engineConfig;
	}
	
	private static Properties loadEngineSpecificProperties(Properties main)
	{
		String alternativeDB = System.getProperty(ALTERNATIVE_DB_CONFIG);
		String ignoreAlt = main.getProperty(PREFIX+IGNORE_ALTERNATIVE_DB_CONFIG);
		if (alternativeDB == null || "true".equals(ignoreAlt))
			return main;
		Properties p = new Properties();
		InputStream is;
		File f = new File(alternativeDB);
		if (f.exists() && f.isFile())
		{
			log.info("Loading alternative DB config from file " + alternativeDB);
			try
			{
				is = new FileInputStream(f);
			} catch (FileNotFoundException e)
			{
				throw new ConfigurationException("Cannot read DB config file", e);
			}
		} else
		{
			String path = "/testDBConfigs/" + alternativeDB + ".conf";
			log.info("Loading alternative DB config from classpath resource " + path);
			is = StorageConfiguration.class.getResourceAsStream(path);
			if (is == null)
				throw new ConfigurationException("Cannot load alternative "
						+ "DB config - no resource " + path);
		}

		try
		{
			Reader isReader = new BufferedReader(new InputStreamReader(is, "UTF-8")); 
			p.load(isReader);
		} catch (Exception e)
		{
			throw new ConfigurationException("Cannot load alternative DB config", e);
		} finally
		{
			try
			{
				is.close();
			} catch (IOException e)
			{
				log.error("Problem closing file", e);
			}
		}
		
		String mainWipeDB = main.getProperty(PREFIX + WIPE_DB_AT_STARTUP);
		if (!p.containsKey(PREFIX+WIPE_DB_AT_STARTUP) && mainWipeDB != null)
			p.setProperty(PREFIX+WIPE_DB_AT_STARTUP, mainWipeDB);
		
		return p;
	}
}
