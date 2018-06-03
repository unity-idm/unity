/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.config.ConfigurationLocationProvider;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

public class TestUnityConfig
{
	@Test
	public void authenticationFlowsAreCorrectlyLoaded() throws ConfigurationException, IOException
	{
		Environment env = mock(Environment.class);
		ConfigurationLocationProvider locProvider = mock(ConfigurationLocationProvider.class);
		Mockito.when(locProvider.getConfigurationLocation()).thenReturn("src/test/resources/testAuthenticatorsSpec.conf");
		
		UnityServerConfiguration config = new UnityServerConfiguration(env, locProvider);
	
		List<String> endpointAuth = config.getEndpointAuth("endpoints.1.");
		assertEquals(4, endpointAuth.size());

	}
}
