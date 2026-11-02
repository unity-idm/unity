package pl.edu.icm.unity.engine.api.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class URLFactoryTest
{
	@Test
	void shouldCreateURLFromAbsoluteAddress() throws MalformedURLException
	{
		// when
		URL url = URLFactory.of("https://example.org/first.png");

		// then
		assertThat(url.toString()).isEqualTo("https://example.org/first.png");
	}

	@ParameterizedTest
	@ValueSource(strings = { "invalid-url", "http://exa mple.org", "unknownproto://example.org" })
	void shouldReportMalformedURLForInvalidAddress(String invalidAddress)
	{
		assertThatThrownBy(() -> URLFactory.of(invalidAddress))
				.isInstanceOf(MalformedURLException.class)
				.hasMessageNotContaining(IllegalArgumentException.class.getName())
				.hasMessageNotContaining("URISyntaxException");
	}

	@Test
	void shouldReportReasonForRelativeAddress()
	{
		assertThatThrownBy(() -> URLFactory.of("invalid-url"))
				.isInstanceOf(MalformedURLException.class)
				.hasMessage("URI is not absolute");
	}
}
