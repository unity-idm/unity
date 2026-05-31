/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.BaseRemoteASProperties;
import pl.edu.icm.unity.oauth.client.profile.OpenIdProfileFetcher;

public class OAuthClientConfigurationParserTest
{
	private static final String GOOGLE = OAuthClientProperties.P + OAuthClientProperties.PROVIDERS + "google.";
	private static final String GITHUB = OAuthClientProperties.P + OAuthClientProperties.PROVIDERS + "github.";

	private final PKIManagement pki = mock(PKIManagement.class);
	private final MessageSource msg = mock(MessageSource.class);
	private final OAuthClientConfigurationParser parser = new OAuthClientConfigurationParser(pki, msg);

	@Test
	public void shouldParseProviderKey()
	{
		OAuthClientConfiguration config = parser.parse(minimalProviderProps(GOOGLE));

		assertThat(config.providers().getKeys()).hasSize(1);
		assertThat(config.providers().getAll().iterator().next().key.asString()).isEqualTo("google");
	}

	@Test
	public void shouldParseClientCredentials()
	{
		OAuthClientConfiguration config = parser.parse(minimalProviderProps(GOOGLE));

		OAuthProviderConfiguration provider = singleProvider(config);
		assertThat(provider.clientId).isEqualTo("testClientId");
		assertThat(provider.clientSecret).isEqualTo("testSecret");
	}

	@Test
	public void shouldParseEndpoints()
	{
		OAuthClientConfiguration config = parser.parse(minimalProviderProps(GOOGLE));

		OAuthProviderConfiguration provider = singleProvider(config);
		assertThat(provider.authorizationEndpoint).isEqualTo("https://auth.example.com");
		assertThat(provider.accessTokenEndpoint).isEqualTo("https://token.example.com");
	}

	@Test
	public void shouldParseScopes()
	{
		Properties p = minimalProviderProps(GOOGLE);
		p.setProperty(GOOGLE + CustomProviderProperties.SCOPES, "openid profile email");

		OAuthProviderConfiguration provider = singleProvider(parser.parse(p));

		assertThat(provider.scopes).isEqualTo("openid profile email");
	}

	@Test
	public void shouldParseOpenIdConnectProvider()
	{
		Properties p = new Properties();
		p.setProperty(GOOGLE + CustomProviderProperties.CLIENT_ID, "testClientId");
		p.setProperty(GOOGLE + CustomProviderProperties.CLIENT_SECRET, "testSecret");
		p.setProperty(GOOGLE + CustomProviderProperties.OPENID_CONNECT, "true");
		p.setProperty(GOOGLE + CustomProviderProperties.OPENID_DISCOVERY,
				"https://accounts.google.com/.well-known/openid-configuration");
		p.setProperty(GOOGLE + CustomProviderProperties.PROVIDER_NAME, "Google");
		p.setProperty(GOOGLE + CommonWebAuthnProperties.TRANSLATION_PROFILE, "testProfile");

		OAuthProviderConfiguration provider = singleProvider(parser.parse(p));

		assertThat(provider.openIdConnect).isTrue();
		assertThat(provider.openIdDiscoveryEndpoint)
				.isEqualTo("https://accounts.google.com/.well-known/openid-configuration");
	}

	@Test
	public void shouldParseFixedACRs()
	{
		Properties p = minimalProviderProps(GOOGLE);
		p.setProperty(GOOGLE + CustomProviderProperties.REQUEST_ACRS_MODE, "FIXED");
		p.setProperty(GOOGLE + CustomProviderProperties.REQUESTED_ACRS + "1", "urn:mace:incommon:iap:silver");
		p.setProperty(GOOGLE + CustomProviderProperties.REQUESTED_ACRS + "2", "urn:mace:incommon:iap:bronze");
		p.setProperty(GOOGLE + CustomProviderProperties.REQUESTED_ACRS_ARE_ESSENTIAL, "true");

		OAuthProviderConfiguration provider = singleProvider(parser.parse(p));

		assertThat(provider.requestACRsMode).isEqualTo(RequestACRsMode.FIXED);
		assertThat(provider.requestedACRs).containsExactlyInAnyOrder(
				"urn:mace:incommon:iap:silver", "urn:mace:incommon:iap:bronze");
		assertThat(provider.requestedACRsAreEssential).isTrue();
	}

	@Test
	public void shouldParseAdditionalAuthzParams()
	{
		Properties p = minimalProviderProps(GOOGLE);
		p.setProperty(GOOGLE + CustomProviderProperties.ADDITIONAL_AUTHZ_PARAMS + "1", "prompt=consent");
		p.setProperty(GOOGLE + CustomProviderProperties.ADDITIONAL_AUTHZ_PARAMS + "2", "hd=example.com");

		OAuthProviderConfiguration provider = singleProvider(parser.parse(p));

		assertThat(provider.additionalAuthzParams).hasSize(2);
		assertThat(provider.additionalAuthzParams).anySatisfy(nvp ->
		{
			assertThat(nvp.getName()).isEqualTo("prompt");
			assertThat(nvp.getValue()).isEqualTo("consent");
		});
		assertThat(provider.additionalAuthzParams).anySatisfy(nvp ->
		{
			assertThat(nvp.getName()).isEqualTo("hd");
			assertThat(nvp.getValue()).isEqualTo("example.com");
		});
	}

	@Test
	public void shouldParseMultipleProviders()
	{
		Properties p = minimalProviderProps(GOOGLE);
		p.putAll(minimalProviderProps(GITHUB));

		OAuthClientConfiguration config = parser.parse(p);

		assertThat(config.providers().getKeys()).hasSize(2);
		assertThat(config.providers().getKeys().stream().map(OAuthProviderKey::asString))
				.containsExactlyInAnyOrder("google", "github");
	}

	@Test
	public void shouldApplyProviderLevelEnableAssociationOverride()
	{
		Properties p = minimalProviderProps(GOOGLE);
		p.setProperty(GOOGLE + CommonWebAuthnProperties.ENABLE_ASSOCIATION, "false");

		OAuthProviderConfiguration provider = singleProvider(parser.parse(p));

		assertThat(provider.enableAssociation).isFalse();
	}

	@Test
	public void shouldInheritDefaultEnableAssociation()
	{
		Properties p = minimalProviderProps(GOOGLE);
		p.setProperty(OAuthClientProperties.P + CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION, "false");

		OAuthClientConfiguration config = parser.parse(p);

		assertThat(config.defaultEnableAssociation).isFalse();
		assertThat(singleProvider(config).enableAssociation).isFalse();
	}

	@Test
	public void shouldParseFederationFields()
	{
		Properties p = minimalProviderProps(GOOGLE);
		p.setProperty(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_MEMBERSHIP_ENABLED, "true");
		p.setProperty(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_SUPERIOR_ENTITY_ID,
				"https://federation.example.com");
		p.setProperty(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_TRUST_ANCHOR_ID,
				"https://anchor.example.com");
		p.setProperty(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_CREDENTIAL, "myCred");

		OAuthClientConfiguration config = parser.parse(p);

		assertThat(config.federationMembershipEnabled).isTrue();
		assertThat(config.federationSuperiorEntityId).isEqualTo("https://federation.example.com");
		assertThat(config.federationTrustAnchorId).isEqualTo("https://anchor.example.com");
		assertThat(config.federationCredential).isEqualTo("myCred");
	}

	@Test
	public void shouldParseClientCredential()
	{
		Properties p = minimalProviderProps(GOOGLE);
		p.setProperty(GOOGLE + CustomProviderProperties.CLIENT_AUTHN_METHOD, "private_key_jwt");
		p.setProperty(GOOGLE + CustomProviderProperties.CLIENT_CREDENTIAL, "myCred");

		OAuthProviderConfiguration provider = singleProvider(parser.parse(p));

		assertThat(provider.clientAuthnMethod).isEqualTo(CustomProviderProperties.ClientAuthnMethod.private_key_jwt);
		assertThat(provider.clientCredential).isEqualTo("myCred");
	}

	@Test
	public void shouldParseAllProviderFields() throws Exception
	{
		when(pki.getValidatorNames()).thenReturn(Set.of("myTruststore"));

		Properties p = new Properties();
		p.setProperty(GOOGLE + CustomProviderProperties.PROVIDER_TYPE, "google");
		p.setProperty(GOOGLE + CustomProviderProperties.CLIENT_ID, "myClientId");
		p.setProperty(GOOGLE + CustomProviderProperties.CLIENT_SECRET, "mySecret");
		p.setProperty(GOOGLE + CustomProviderProperties.PROVIDER_LOCATION, "https://auth.example.com");
		p.setProperty(GOOGLE + CustomProviderProperties.ACCESS_TOKEN_ENDPOINT, "https://token.example.com");
		p.setProperty(GOOGLE + BaseRemoteASProperties.PROFILE_ENDPOINT, "https://userinfo.example.com");
		p.setProperty(GOOGLE + CustomProviderProperties.PROVIDER_NAME, "My Provider");
		p.setProperty(GOOGLE + CustomProviderProperties.ICON_URL, "https://icon.example.com/icon.png");
		p.setProperty(GOOGLE + CustomProviderProperties.SCOPES, "openid email profile");
		p.setProperty(GOOGLE + CustomProviderProperties.ACCESS_TOKEN_FORMAT, "httpParams");
		p.setProperty(GOOGLE + CustomProviderProperties.CLIENT_AUTHN_METHOD, "private_key_jwt");
		p.setProperty(GOOGLE + CustomProviderProperties.CLIENT_CREDENTIAL, "myCred");
		p.setProperty(GOOGLE + BaseRemoteASProperties.CLIENT_AUTHN_MODE, "secretPost");
		p.setProperty(GOOGLE + BaseRemoteASProperties.CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS, "secretPost");
		p.setProperty(GOOGLE + BaseRemoteASProperties.CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS, "post");
		p.setProperty(GOOGLE + BaseRemoteASProperties.CLIENT_TRUSTSTORE, "myTruststore");
		p.setProperty(GOOGLE + BaseRemoteASProperties.CLIENT_HOSTNAME_CHECKING, "WARN");
		p.setProperty(GOOGLE + CustomProviderProperties.REQUEST_ACRS_MODE, "FIXED");
		p.setProperty(GOOGLE + CustomProviderProperties.REQUESTED_ACRS + "1", "acr1");
		p.setProperty(GOOGLE + CustomProviderProperties.REQUESTED_ACRS_ARE_ESSENTIAL, "true");
		p.setProperty(GOOGLE + CustomProviderProperties.ADDITIONAL_AUTHZ_PARAMS + "1", "prompt=consent");
		p.setProperty(GOOGLE + CommonWebAuthnProperties.TRANSLATION_PROFILE, "myProfile");
		p.setProperty(GOOGLE + CommonWebAuthnProperties.REGISTRATION_FORM, "myForm");
		p.setProperty(GOOGLE + CommonWebAuthnProperties.ENABLE_ASSOCIATION, "false");

		OAuthProviderConfiguration provider = singleProvider(parser.parse(p));

		assertThat(provider.key.asString()).isEqualTo("google");
		assertThat(provider.providerType).isEqualTo(OAuthClientProperties.Providers.google);
		assertThat(provider.clientId).isEqualTo("myClientId");
		assertThat(provider.clientSecret).isEqualTo("mySecret");
		assertThat(provider.authorizationEndpoint).isEqualTo("https://auth.example.com");
		assertThat(provider.accessTokenEndpoint).isEqualTo("https://token.example.com");
		assertThat(provider.userInfoEndpoints).containsExactly("https://userinfo.example.com");
		assertThat(provider.name.getDefaultValue()).isEqualTo("My Provider");
		assertThat(provider.iconUrl.getDefaultValue()).isEqualTo("https://icon.example.com/icon.png");
		assertThat(provider.openIdConnect).isTrue(); // google provider type forces OpenID Connect mode
		assertThat(provider.scopes).isEqualTo("openid email profile");
		assertThat(provider.accessTokenFormat).isEqualTo(CustomProviderProperties.AccessTokenFormat.httpParams);
		assertThat(provider.clientAuthnMethod).isEqualTo(CustomProviderProperties.ClientAuthnMethod.private_key_jwt);
		assertThat(provider.clientCredential).isEqualTo("myCred");
		assertThat(provider.clientAuthnMode).isEqualTo(Optional.of(CustomProviderProperties.ClientAuthnMode.secretPost));
		assertThat(provider.clientAuthnModeForProfileAccess).isEqualTo(CustomProviderProperties.ClientAuthnMode.secretPost);
		assertThat(provider.clientHttpMethodForProfileAccess).isEqualTo(Method.POST);
		assertThat(provider.truststoreName).isEqualTo("myTruststore");
		assertThat(provider.hostNameCheckingMode).isEqualTo(ServerHostnameCheckingMode.WARN);
		assertThat(provider.requestACRsMode).isEqualTo(RequestACRsMode.FIXED);
		assertThat(provider.requestedACRs).containsExactly("acr1");
		assertThat(provider.requestedACRsAreEssential).isTrue();
		assertThat(provider.additionalAuthzParams).hasSize(1);
		assertThat(provider.translationProfile).isNotNull();
		assertThat(provider.registrationForm).isEqualTo("myForm");
		assertThat(provider.enableAssociation).isFalse();
		assertThat(provider.userAttributesResolver).isInstanceOf(OpenIdProfileFetcher.class);
	}

	private Properties minimalProviderProps(String providerPrefix)
	{
		Properties p = new Properties();
		p.setProperty(providerPrefix + CustomProviderProperties.CLIENT_ID, "testClientId");
		p.setProperty(providerPrefix + CustomProviderProperties.CLIENT_SECRET, "testSecret");
		p.setProperty(providerPrefix + CustomProviderProperties.PROVIDER_LOCATION, "https://auth.example.com");
		p.setProperty(providerPrefix + CustomProviderProperties.ACCESS_TOKEN_ENDPOINT, "https://token.example.com");
		p.setProperty(providerPrefix + CustomProviderProperties.PROVIDER_NAME, "Test Provider");
		p.setProperty(providerPrefix + CommonWebAuthnProperties.TRANSLATION_PROFILE, "testProfile");
		return p;
	}

	private OAuthProviderConfiguration singleProvider(OAuthClientConfiguration config)
	{
		assertThat(config.providers().getAll()).hasSize(1);
		return config.providers().getAll().iterator().next();
	}
}
