/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import pl.edu.icm.unity.engine.api.config.ConfigurationLocationProvider;

public class MockDosConfig implements ConfigurationLocationProvider
{
	@Override
	public String getConfigurationLocation()
	{
		return "src/test/resources/dosTest.conf";
	}
}
