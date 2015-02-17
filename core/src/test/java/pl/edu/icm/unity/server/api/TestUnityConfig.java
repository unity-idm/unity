/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

import pl.edu.icm.unity.server.utils.ConfigurationLocationProvider;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import eu.unicore.util.configuration.ConfigurationException;

public class TestUnityConfig
{
	@Test
	public void authenticatorsAreCorrectlyLoaded() throws ConfigurationException, IOException
	{
		Environment env = mock(Environment.class);
		ConfigurationLocationProvider locProvider = mock(ConfigurationLocationProvider.class);
		Mockito.when(locProvider.getConfigurationLocation()).thenReturn("src/test/resources/testAuthenticatorsSpec.conf");
		
		UnityServerConfiguration config = new UnityServerConfiguration(env, locProvider);
		
		List<AuthenticationOptionDescription> endpointAuth = config.getEndpointAuth("endpoints.1.");
		assertEquals(3, endpointAuth.size());
		assertEquals("a1", endpointAuth.get(0).getPrimaryAuthenticator());
		assertNull(endpointAuth.get(0).getMandatory2ndAuthenticator());
		
		assertEquals("a2", endpointAuth.get(1).getPrimaryAuthenticator());
		assertEquals("a3", endpointAuth.get(1).getMandatory2ndAuthenticator());
		
		assertEquals("a4", endpointAuth.get(2).getPrimaryAuthenticator());
		assertNull(endpointAuth.get(2).getMandatory2ndAuthenticator());
	}
}
