package pl.edu.icm.unity.engine.api.authn;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.Cookie;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;

class LastAuthenticationCookieTest
{
	@Test
	void shouldCreateSecureHttpOnlyCookieWithEndpointScope()
	{
		AuthenticationOptionKey option = new AuthenticationOptionKey("password", "option");

		Cookie cookie = LastAuthenticationCookie.createLastIdpCookie("/home", option).orElseThrow();

		assertThat(cookie.getName()).isEqualTo(LastAuthenticationCookie.LAST_AUTHN_COOKIE);
		assertThat(cookie.getValue()).isEqualTo(option.toStringEncodedKey());
		assertThat(cookie.getPath()).isEqualTo("/home");
		assertThat(cookie.getMaxAge()).isEqualTo((int) Duration.ofDays(30).toSeconds());
		assertThat(cookie.getSecure()).isTrue();
		assertThat(cookie.isHttpOnly()).isTrue();
	}

	@Test
	void shouldNotCreateCookieWithoutEndpointPath()
	{
		AuthenticationOptionKey option = AuthenticationOptionKey.authenticatorOnlyKey("password");

		assertThat(LastAuthenticationCookie.createLastIdpCookie(null, option)).isEmpty();
	}

	@Test
	void shouldNotCreateCookieWithoutAuthenticationOption()
	{
		assertThat(LastAuthenticationCookie.createLastIdpCookie("/home", null)).isEmpty();
	}
}
