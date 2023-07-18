/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import eu.unicore.util.configuration.ConfigurationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class InitAllowedRequestersTest
{
	@Test
	public void shouldParseAllowedRequesters()
	{
		Map<Integer, String> parsed = SAMLIdPConfiguration.initAllowedRequesters(Set.of("[1]http://url", "[11]http://url2"));

		assertThat(parsed.size()).isEqualTo(2);
		assertThat(parsed.get(1)).isEqualTo("http://url");
		assertThat(parsed.get(11)).isEqualTo("http://url2");
	}

	@Test
	public void shouldProtestOnInvalidSyntax()
	{
		Assertions.catchThrowable(() -> SAMLIdPConfiguration.initAllowedRequesters(Set.of("[]http://url2")))
				.addSuppressed(new ConfigurationException());
	}

	@Test
	public void shouldProtestOnInvalidSyntax2()
	{
		Assertions.catchThrowable(() -> SAMLIdPConfiguration.initAllowedRequesters(Set.of("[sd]http://url2")))
				.addSuppressed(new ConfigurationException());
	}

}
