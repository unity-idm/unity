/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.federation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider;

class FederationClientAttributesMapperTest
{
	private static final String GROUP = "/oauth-clients";
	private static final String CLIENT_ID = "https://client.example.com";

	@Test
	void shouldNotSetAllowedScopesAttribute_whenClientDeclaresNoneAndAllowAnyScopes()
	{
		OIDCClientMetadata meta = new OIDCClientMetadata();

		List<Attribute> attrs = FederationClientAttributesMapper.toOAuthAttributes(meta, GROUP, CLIENT_ID,
				new OAuthFederationClientDefaults(true, List.of()));

		assertThat(findAllowedScopes(attrs)).isEmpty();
	}

	@Test
	void shouldUseDeclaredScopes_whenAllowAnyScopes()
	{
		OIDCClientMetadata meta = new OIDCClientMetadata();
		meta.setScope(Scope.parse("openid profile"));

		List<Attribute> attrs = FederationClientAttributesMapper.toOAuthAttributes(meta, GROUP, CLIENT_ID,
				new OAuthFederationClientDefaults(true, List.of()));

		assertThat(allowedScopeValues(attrs)).containsExactlyInAnyOrder("openid", "profile");
	}

	@Test
	void shouldIgnoreConfiguredDefaultScopes_whenAllowAnyScopesIsTrue()
	{
		OIDCClientMetadata meta = new OIDCClientMetadata();
		meta.setScope(Scope.parse("profile"));

		List<Attribute> attrs = FederationClientAttributesMapper.toOAuthAttributes(meta, GROUP, CLIENT_ID,
				new OAuthFederationClientDefaults(true, List.of("openid", "email")));

		assertThat(allowedScopeValues(attrs)).containsExactly("profile");
	}

	@Test
	void shouldUseDefaultScopes_whenClientDeclaresNoScopesAndAllowAnyScopesIsFalse()
	{
		OIDCClientMetadata meta = new OIDCClientMetadata();

		List<Attribute> attrs = FederationClientAttributesMapper.toOAuthAttributes(meta, GROUP, CLIENT_ID,
				new OAuthFederationClientDefaults(false, List.of("openid", "email")));

		assertThat(allowedScopeValues(attrs)).containsExactlyInAnyOrder("openid", "email");
	}

	@Test
	void shouldIntersectDeclaredAndDefaultScopes_whenBothPresentAndAllowAnyScopesIsFalse()
	{
		OIDCClientMetadata meta = new OIDCClientMetadata();
		meta.setScope(Scope.parse("openid profile email"));

		List<Attribute> attrs = FederationClientAttributesMapper.toOAuthAttributes(meta, GROUP, CLIENT_ID,
				new OAuthFederationClientDefaults(false, List.of("openid", "email")));

		assertThat(allowedScopeValues(attrs)).containsExactlyInAnyOrder("openid", "email");
	}

	@Test
	void shouldSetExplicitlyEmptyAllowedScopesAttribute_whenIntersectionIsEmpty()
	{
		OIDCClientMetadata meta = new OIDCClientMetadata();
		meta.setScope(Scope.parse("profile"));

		List<Attribute> attrs = FederationClientAttributesMapper.toOAuthAttributes(meta, GROUP, CLIENT_ID,
				new OAuthFederationClientDefaults(false, List.of("openid", "email")));

		Optional<Attribute> allowedScopes = findAllowedScopes(attrs);
		assertThat(allowedScopes).isPresent();
		assertThat(allowedScopes.get().getValues()).isEmpty();
	}

	@Test
	void shouldUseDeclaredScopes_whenFullySubsetOfDefaults()
	{
		OIDCClientMetadata meta = new OIDCClientMetadata();
		meta.setScope(Scope.parse("openid"));

		List<Attribute> attrs = FederationClientAttributesMapper.toOAuthAttributes(meta, GROUP, CLIENT_ID,
				new OAuthFederationClientDefaults(false, List.of("openid", "email", "profile")));

		assertThat(allowedScopeValues(attrs)).containsExactly("openid");
	}

	private static Optional<Attribute> findAllowedScopes(List<Attribute> attrs)
	{
		return attrs.stream()
				.filter(a -> OAuthSystemAttributesProvider.ALLOWED_SCOPES.equals(a.getName()))
				.findFirst();
	}

	private static List<String> allowedScopeValues(List<Attribute> attrs)
	{
		return findAllowedScopes(attrs)
				.orElseThrow(() -> new AssertionError("ALLOWED_SCOPES attribute not set"))
				.getValues();
	}
}
