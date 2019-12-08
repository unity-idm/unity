/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.home.console;

import static org.mockito.Mockito.mock;
import static pl.edu.icm.unity.configtester.ConfigurationComparator.createComparator;
import static pl.edu.icm.unity.home.HomeEndpointProperties.META;
import static pl.edu.icm.unity.home.HomeEndpointProperties.PREFIX;

import java.util.Arrays;
import java.util.Properties;

import org.junit.Test;

import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Group;

public class HomeConfigurationTest
{
	private UnityMessageSource msg = mock(UnityMessageSource.class);

	@Test
	public void serializationIsIdempotentForMinimalConfig() throws Exception
	{
		Properties sourceCfg = ConfigurationGenerator.generateMinimalWithoutDefaults(PREFIX, META).get();
		HomeServiceConfiguration processor = new HomeServiceConfiguration();

		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, "extraTab",
				Arrays.asList(new Group("/"), new Group("/A")));
		String converted = processor.toProperties("extraTab");
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

		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, "extraTab",
				Arrays.asList(new Group("/"), new Group("/A")));
		String converted = processor.toProperties("extraTab");
		Properties result = ConfigurationComparator.fromString(converted, PREFIX).get();
		createComparator(PREFIX, META).ignoringSuperflous("attributes.1.editable", "attributes.1.showGroup")
				.withExpectedChange("attributes.1.group", "/foo").checkMatching(result, sourceCfg);
	}

	@Test
	public void serializationIsIdempotentForCompleteNonDefaultConfig() throws Exception
	{
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, META)
				.update("disabledComponents.1", "extraTab")
				.update("enableProjectManagementLink", "true").get();
		HomeServiceConfiguration processor = new HomeServiceConfiguration();
		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg, "extraTab",
				Arrays.asList(new Group("/"), new Group("/A")));
		String converted = processor.toProperties("extraTab");
		Properties result = ConfigurationComparator.fromString(converted, PREFIX).get();

		createComparator(PREFIX, META).ignoringSuperflous("attributes.1.editable", "attributes.1.showGroup")
				.withExpectedChange("attributes.1.group", "/foo").checkMatching(result, sourceCfg);
	}
}
