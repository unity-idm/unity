/*
 * Copyright (c) 2026 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.files;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SsrfProtectionTest
{
	@ParameterizedTest
	@ValueSource(strings = {
			"http://127.0.0.1/logo.png",
			"http://127.55.1.2/logo.png",
			"http://localhost/logo.png",
			"http://169.254.169.254/latest/meta-data/",
			"http://10.0.0.5/logo.png",
			"http://172.16.5.5/logo.png",
			"http://192.168.1.1/logo.png",
			"http://0.0.0.0/logo.png",
			"http://100.64.0.1/logo.png",
			"http://192.0.2.10/logo.png",
			"http://198.51.100.10/logo.png",
			"http://203.0.113.10/logo.png",
			"http://198.18.0.1/logo.png",
			"http://255.255.255.255/logo.png",
			"http://240.0.0.1/logo.png",
			"http://[::1]/logo.png",
			"http://[fe80::1]/logo.png",
			"http://[fc00::1]/logo.png",
			"http://[2001:db8::1]/logo.png",
			"http://[::ffff:127.0.0.1]/logo.png",
			"http://[::ffff:169.254.169.254]/logo.png",
	})
	void shouldRejectInternalDestination(String uri) throws Exception
	{
		assertThatThrownBy(() -> SsrfProtection.assertNoInternalDestination(URI.create(uri)))
				.isInstanceOf(IOException.class);
	}

	@Test
	void shouldRejectFileScheme()
	{
		assertThatThrownBy(() -> SsrfProtection.assertNoInternalDestination(URI.create("file:///etc/passwd")))
				.isInstanceOf(IOException.class);
	}

	@Test
	void shouldRejectDataScheme()
	{
		assertThatThrownBy(() -> SsrfProtection.assertNoInternalDestination(URI.create("data:image/png;base64,AA==")))
				.isInstanceOf(IOException.class);
	}

	@Test
	void shouldRejectUnresolvableHost()
	{
		assertThatThrownBy(() -> SsrfProtection.assertNoInternalDestination(
				URI.create("http://this-host-should-never-resolve.invalid/logo.png")))
				.isInstanceOf(IOException.class);
	}

	@Test
	void shouldAllowOrdinaryPublicHttpsDestination()
	{
		// a numeric public IP avoids relying on real DNS in the test environment
		assertThatCode(() -> SsrfProtection.assertNoInternalDestination(URI.create("https://8.8.8.8/logo.png")))
				.doesNotThrowAnyException();
	}
}
