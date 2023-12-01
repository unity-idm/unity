/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest.console;

import static io.imunity.upman.rest.UpmanRestEndpointProperties.META;
import static io.imunity.upman.rest.UpmanRestEndpointProperties.PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.configtester.ConfigurationComparator;
import pl.edu.icm.unity.configtester.ConfigurationGenerator;

public class UpmanRestEndpointPropertiesTest
{
	private final MessageSource msg = mock(MessageSource.class);

	@Test
	public void shouldSerialize()
	{
		Properties sourceCfg = ConfigurationGenerator.generateCompleteWithNonDefaults(PREFIX, META)
			.update("rootGroup", "/A")
			.update("authorizationGroup", "/A/B")
			.update("allowedCorsOrigins.1", "abc")
			.update("allowedCorsOrigins.2", "cde")
			.update("allowedCorsHeaders.1", "zzz")
			.update("allowedCorsHeaders.2", "sss")
			.get();

		UpmanRestServiceConfiguration processor = new UpmanRestServiceConfiguration();

		processor.fromProperties(ConfigurationComparator.getAsString(sourceCfg), msg);
		String converted = processor.toProperties();

		Properties result = ConfigurationComparator.fromString(converted, PREFIX).get();

		assertThat(result.get(PREFIX + "rootGroup")).isEqualTo("/A");
		assertThat(result.get(PREFIX + "authorizationGroup")).isEqualTo("/A/B");
		assertThat(result.get(PREFIX + "allowedCorsOrigins.1")).isEqualTo("abc");
		assertThat(result.get(PREFIX + "allowedCorsOrigins.2")).isEqualTo("cde");
		assertThat(result.get(PREFIX + "allowedCorsHeaders.1")).isEqualTo("zzz");
		assertThat(result.get(PREFIX + "allowedCorsHeaders.2")).isEqualTo("sss");
	}
}