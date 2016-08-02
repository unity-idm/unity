/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Returns the configuration file: use a reselected one with property or the default one if not set.
 * @author K. Benedyczak
 */
@Component
public class DefaultConfigurationLocation implements ConfigurationLocationProvider
{
	public static final String CONFIG_FILE_PROP = "unityConfig";
	
	@Autowired
	private Environment env;
	
	@Override
	public String getConfigurationLocation()
	{
		String configLocation = env.getProperty(CONFIG_FILE_PROP);
		
		return configLocation == null ? UnityServerConfiguration.CONFIGURATION_FILE : configLocation;
	}
}
