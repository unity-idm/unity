/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.*;
import org.junit.Test;

import eu.unicore.util.configuration.ConfigurationException;

import pl.edu.icm.unity.server.utils.UnityPKIConfiguration;

public class TestPKIConfig
{
	@Test
	public void test() throws ConfigurationException, IOException
	{
		Properties p = new Properties();
		p.setProperty(UnityPKIConfiguration.P + UnityPKIConfiguration.TRUSTSTORES + "m." + "type", "keystore");
		p.setProperty(UnityPKIConfiguration.P + UnityPKIConfiguration.CREDENTIALS + "n." + "path", "sss");
		UnityPKIConfiguration cfg = new UnityPKIConfiguration(p);
		Set<String> valNames = cfg.getStructuredListKeys(UnityPKIConfiguration.TRUSTSTORES);
		assertEquals(1, valNames.size());
		assertEquals("m", cfg.getTruststoreName(valNames.iterator().next()));
		
		Set<String> credNames = cfg.getStructuredListKeys(UnityPKIConfiguration.CREDENTIALS);
		assertEquals(1, credNames.size());
		assertEquals("n", cfg.getTruststoreName(credNames.iterator().next()));
	}
}
