/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.console;

import org.junit.jupiter.api.Test;
import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;

import java.util.Properties;

import static io.imunity.upman.UpmanEndpointProperties.META;
import static io.imunity.upman.UpmanEndpointProperties.PREFIX;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;

public class UpmanConfigurationTest {

	@Test
	public void serializationIsIdempotentForMinimalConfig() throws Exception {
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithoutDefaults(PREFIX, META).get();
		UpmanServiceConfiguration processor = new UpmanServiceConfiguration();

		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg));
		String converted = processor.toProperties();
		Properties result = ConfigurationComparator.fromString(converted, PREFIX).get();
		createComparator(PREFIX, META).checkMatching(result, sourceCfg);
	}

	@Test
	public void serializationIsIdempotentForMinimalExplicitDefaultsConfig() throws Exception {
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithDefaults(PREFIX, META)

				.get();
		UpmanServiceConfiguration processor = new UpmanServiceConfiguration();

		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg));
		String converted = processor.toProperties();
		Properties result = ConfigurationComparator.fromString(converted, PREFIX).get();
		createComparator(PREFIX, META).checkMatching(result, sourceCfg);
	}

	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfig() throws Exception {
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, META)
				.update("enableHomeLink", "true").update("homeEndpoint", "home").get();
		UpmanServiceConfiguration processor = new UpmanServiceConfiguration();
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg));
		String converted = processor.toProperties();
		Properties result = ConfigurationComparator.fromString(converted, PREFIX).get();

		createComparator(PREFIX, META).checkMatching(result, sourceCfg);
	}
}
