/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Returns the default configuration file
 * @author K. Benedyczak
 */
@Component
@Profile(UnityServerConfiguration.PROFILE_PRODUCTION)
public class DefaultConfigurationLocation implements ConfigurationLocationProvider
{
	@Override
	public String getConfigurationLocation()
	{
		return UnityServerConfiguration.CONFIGURATION_FILE;
	}
}
