/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.config.UnityPKIConfiguration;

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
		assertThat(valNames).hasSize(1);
		assertThat(cfg.getTruststoreName(valNames.iterator().next())).isEqualTo("m");
		
		Set<String> credNames = cfg.getStructuredListKeys(UnityPKIConfiguration.CREDENTIALS);
		assertThat(credNames).hasSize(1);
		assertThat(cfg.getTruststoreName(credNames.iterator().next())).isEqualTo("n");
	}
}
