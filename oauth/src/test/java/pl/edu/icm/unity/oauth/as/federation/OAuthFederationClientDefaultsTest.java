/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.federation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.oauth.as.OAuthASProperties;

class OAuthFederationClientDefaultsTest
{
	@Test
	void shouldNormalizeNullAllowedScopesToEmptyList()
	{
		OAuthFederationClientDefaults defaults = new OAuthFederationClientDefaults(true, null);

		assertThat(defaults.allowedScopes()).isEmpty();
	}

	@Test
	void shouldDefaultToAllowAnyScopesWhenNotConfigured()
	{
		Properties config = new Properties();
		config.setProperty(OAuthASProperties.P + OAuthASProperties.ISSUER_URI, "http://unity.example.com");
		OAuthASProperties props = new OAuthASProperties(config, null, null);

		OAuthFederationClientDefaults defaults = OAuthFederationClientDefaults.from(props);

		assertThat(defaults.allowAnyScopes()).isTrue();
		assertThat(defaults.allowedScopes()).isEmpty();
	}

	@Test
	void shouldParseAllowAnyScopesDisabled()
	{
		Properties config = new Properties();
		config.setProperty(OAuthASProperties.P + OAuthASProperties.ISSUER_URI, "http://unity.example.com");
		config.setProperty(OAuthASProperties.P + OAuthASProperties.FEDERATION_ALLOW_ANY_SCOPES, "false");
		OAuthASProperties props = new OAuthASProperties(config, null, null);

		OAuthFederationClientDefaults defaults = OAuthFederationClientDefaults.from(props);

		assertThat(defaults.allowAnyScopes()).isFalse();
	}

	@Test
	void shouldParseConfiguredAllowedScopes()
	{
		Properties config = new Properties();
		config.setProperty(OAuthASProperties.P + OAuthASProperties.ISSUER_URI, "http://unity.example.com");
		config.setProperty(OAuthASProperties.P + OAuthASProperties.FEDERATION_ALLOW_ANY_SCOPES, "false");
		config.setProperty(OAuthASProperties.P + OAuthASProperties.FEDERATION_ALLOWED_SCOPES + "1", "openid");
		config.setProperty(OAuthASProperties.P + OAuthASProperties.FEDERATION_ALLOWED_SCOPES + "2", "profile");
		OAuthASProperties props = new OAuthASProperties(config, null, null);

		OAuthFederationClientDefaults defaults = OAuthFederationClientDefaults.from(props);

		assertThat(defaults.allowedScopes()).containsExactly("openid", "profile");
	}

	@Test
	void shouldReturnImmutableAllowedScopes()
	{
		List<String> mutable = new java.util.ArrayList<>(List.of("openid"));
		OAuthFederationClientDefaults defaults = new OAuthFederationClientDefaults(false, mutable);
		mutable.add("profile");

		assertThat(defaults.allowedScopes()).containsExactly("openid");
	}
}
