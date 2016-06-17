/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.ConfigurationLocationProvider;

@Component
public class MockConfig implements ConfigurationLocationProvider
{
	@Override
	public String getConfigurationLocation()
	{
		return "src/test/resources/unityServer.conf";
	}
}
