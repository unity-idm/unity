/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server;

import pl.edu.icm.unity.server.utils.ConfigurationLocationProvider;

public class MockDosConfig implements ConfigurationLocationProvider
{
	@Override
	public String getConfigurationLocation()
	{
		return "src/test/resources/dosTest.conf";
	}
}
