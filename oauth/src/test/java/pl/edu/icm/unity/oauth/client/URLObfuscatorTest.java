package pl.edu.icm.unity.oauth.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

class URLObfuscatorTest
{
	@Test
	void shouldObfuscateSecretParameters() throws URISyntaxException, MalformedURLException
	{
		String obfuscatedURL = URLObfuscator.obfuscateURLParams(
				new URI("http://localhost:8080/some/path?client_secret=super-secret&not_secret=baaar").toURL());

		assertThat(obfuscatedURL).doesNotContain("super-secret")
				.contains("http://localhost:8080/some/path")
				.contains("not_secret=baaar")
				.contains("client_secret=");

	}

	@Test
	void shouldSkipObfuscationWithoutQuery() throws URISyntaxException, MalformedURLException
	{
		String obfuscatedURL = URLObfuscator.obfuscateURLParams(
				new URI("http://localhost:8080/some/path").toURL());

		assertThat(obfuscatedURL).isEqualTo("http://localhost:8080/some/path");
	}
}
