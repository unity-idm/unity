/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import org.springframework.stereotype.Component;

/**
 * Returns the default configuraiton file
 * @author K. Benedyczak
 */
@Component
public class DefaultConfigurationLocation implements ConfigurationLocationProvider
{
	@Override
	public String getConfigurationLocation()
	{
		return UnityServerConfiguration.CONFIGURATION_FILE;
	}
}
