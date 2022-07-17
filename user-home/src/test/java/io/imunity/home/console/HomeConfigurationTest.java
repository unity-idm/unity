/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.console;

import static io.imunity.home.HomeEndpointProperties.META;
import static io.imunity.home.HomeEndpointProperties.PREFIX;
import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;

import java.util.Arrays;
import java.util.Properties;

import org.junit.Test;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.types.basic.Group;

public class HomeConfigurationTest
{
	private MessageSource msg = mock(MessageSource.class);

	@Test
	public void serializationIsIdempotentForMinimalConfig() throws Exception
	{
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithoutDefaults(PREFIX, META).get();
		HomeServiceConfiguration processor = new HomeServiceConfiguration();

		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg,
				Arrays.asList(new Group("/"), new Group("/A")));
		String converted = processor.toProperties();
		Properties result = ConfigurationComparator.fromString(converted, PREFIX).get();
		createComparator(PREFIX, META).ignoringSuperflous("attributes.1.editable", "attributes.1.showGroup")
				.withExpectedChange("attributes.1.group", "/foo").checkMatching(result, sourceCfg);
	}

	@Test
	public void serializationIsIdempotentForMinimalExplicitDefaultsConfig() throws Exception
	{
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithDefaults(PREFIX, META)

				.get();
		HomeServiceConfiguration processor = new HomeServiceConfiguration();

		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, 
				Arrays.asList(new Group("/"), new Group("/A")));
		String converted = processor.toProperties();
		Properties result = ConfigurationComparator.fromString(converted, PREFIX).get();
		createComparator(PREFIX, META).ignoringSuperflous("attributes.1.editable", "attributes.1.showGroup")
				.withExpectedChange("attributes.1.group", "/foo").checkMatching(result, sourceCfg);
	}

	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfig() throws Exception
	{
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, META)
				.update("disabledComponents.1", "accountUpdateTab")
				.update("enableProjectManagementLink", "true").get();
		HomeServiceConfiguration processor = new HomeServiceConfiguration();
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg,
				Arrays.asList(new Group("/"), new Group("/A")));
		String converted = processor.toProperties();
		Properties result = ConfigurationComparator.fromString(converted, PREFIX).get();

		createComparator(PREFIX, META).ignoringSuperflous("attributes.1.editable", "attributes.1.showGroup")
				.withExpectedChange("attributes.1.group", "/foo").checkMatching(result, sourceCfg);
	}
}
