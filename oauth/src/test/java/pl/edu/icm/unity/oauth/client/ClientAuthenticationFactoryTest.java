/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMethod;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;

public class ClientAuthenticationFactoryTest
{
	private static final String RSA_KEYSTORE = "src/test/resources/pki/demoKeystore.p12";
	private static final String RSA_KEYSTORE_PASS = "the!unity";
	private static final String EC_KEYSTORE = "src/test/resources/demoECKey.p12";
	private static final String EC_KEYSTORE_PASS = "123456";
	private static final URI TOKEN_ENDPOINT = URI.create("https://provider.example.com/token");

	@Test
	void shouldBuildPrivateKeyJWTWithRS256ForRSACredential() throws Exception
	{
		PKIManagement pki = pkiManWith(rsaCredential());
		ClientAuthenticationFactory factory = new ClientAuthenticationFactory(pki);

		var result = factory.build(providerCfgWithPrivateKeyJwt("clientId", "myCred"),
				TOKEN_ENDPOINT, ClientAuthnMode.secretBasic);

		assertThat(result).isInstanceOf(PrivateKeyJWT.class);
		assertThat(((PrivateKeyJWT) result).getJWTAuthenticationClaimsSet().getClientID().getValue())
				.isEqualTo("clientId");
		assertThat(result.getMethod()).isEqualTo(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
	}

	@Test
	void shouldBuildPrivateKeyJWTWithES256ForECCredential() throws Exception
	{
		PKIManagement pki = pkiManWith(ecCredential());
		ClientAuthenticationFactory factory = new ClientAuthenticationFactory(pki);

		var result = factory.build(providerCfgWithPrivateKeyJwt("clientId", "myCred"),
				TOKEN_ENDPOINT, ClientAuthnMode.secretBasic);

		assertThat(result).isInstanceOf(PrivateKeyJWT.class);
		assertThat(result.getMethod()).isEqualTo(ClientAuthenticationMethod.PRIVATE_KEY_JWT);
	}

	@Test
	void privateKeyJwtShouldIncludeIatClaim() throws Exception
	{
		PKIManagement pki = pkiManWith(rsaCredential());
		ClientAuthenticationFactory factory = new ClientAuthenticationFactory(pki);

		var result = (PrivateKeyJWT) factory.build(providerCfgWithPrivateKeyJwt("clientId", "myCred"),
				TOKEN_ENDPOINT, ClientAuthnMode.secretBasic);

		assertThat(result.getJWTAuthenticationClaimsSet().getIssueTime()).isNotNull();
		long lifetimeMs = result.getJWTAuthenticationClaimsSet().getExpirationTime().getTime()
				- result.getJWTAuthenticationClaimsSet().getIssueTime().getTime();
		assertThat(lifetimeMs).isEqualTo(ClientAuthenticationFactory.ASSERTION_LIFETIME_MS);
	}

	@Test
	void shouldBuildClientSecretBasicForSecretBasicMode() throws Exception
	{
		ClientAuthenticationFactory factory = new ClientAuthenticationFactory(mock(PKIManagement.class));

		var result = factory.build(providerCfgWithSecret("clientId", "secret"),
				TOKEN_ENDPOINT, ClientAuthnMode.secretBasic);

		assertThat(result).isInstanceOf(ClientSecretBasic.class);
	}

	@Test
	void shouldBuildClientSecretPostForSecretPostMode() throws Exception
	{
		ClientAuthenticationFactory factory = new ClientAuthenticationFactory(mock(PKIManagement.class));

		var result = factory.build(providerCfgWithSecret("clientId", "secret"),
				TOKEN_ENDPOINT, ClientAuthnMode.secretPost);

		assertThat(result).isInstanceOf(ClientSecretPost.class);
	}

	@Test
	void shouldDeriveRS256ForRSAKey() throws Exception
	{
		KeyPair rsa = KeyPairGenerator.getInstance("RSA").generateKeyPair();

		assertThat(ClientAuthenticationFactory.deriveJWSAlgorithm(rsa.getPrivate()))
				.isEqualTo(JWSAlgorithm.RS256);
	}

	@Test
	void shouldDeriveES256ForECKey() throws Exception
	{
		KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
		gen.initialize(256);
		KeyPair ec = gen.generateKeyPair();

		assertThat(ClientAuthenticationFactory.deriveJWSAlgorithm(ec.getPrivate()))
				.isEqualTo(JWSAlgorithm.ES256);
	}

	@Test
	void shouldThrowInternalExceptionForUnsupportedKeyType() throws Exception
	{
		KeyPair dsa = KeyPairGenerator.getInstance("DSA").generateKeyPair();

		assertThatThrownBy(() -> ClientAuthenticationFactory.deriveJWSAlgorithm(dsa.getPrivate()))
				.isInstanceOf(InternalException.class)
				.hasMessageContaining("DSA");
	}

	@Test
	void establishOpenIDAuthnModeForPrivateKeyJwt_shouldAcceptWhenProviderSupportsIt() throws Exception
	{
		OIDCProviderMetadata meta = mockProviderMetaWithMethods(
				List.of(ClientAuthenticationMethod.PRIVATE_KEY_JWT));
		OAuth2Verificator verificator = mockVerificator();

		verificator.establishOpenIDAuthnModeForPrivateKeyJwt(meta);
	}

	@Test
	void establishOpenIDAuthnModeForPrivateKeyJwt_shouldAcceptWhenProviderHasNullMethods() throws Exception
	{
		OIDCProviderMetadata meta = mockProviderMetaWithMethods(null);
		OAuth2Verificator verificator = mockVerificator();

		verificator.establishOpenIDAuthnModeForPrivateKeyJwt(meta);
	}

	@Test
	void establishOpenIDAuthnModeForPrivateKeyJwt_shouldThrowWhenProviderDoesNotSupportIt()
	{
		OIDCProviderMetadata meta = mockProviderMetaWithMethods(
				List.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));
		OAuth2Verificator verificator = mockVerificator();

		assertThatThrownBy(() -> verificator.establishOpenIDAuthnModeForPrivateKeyJwt(meta))
				.isInstanceOf(AuthenticationException.class)
				.hasMessageContaining("private_key_jwt");
	}

	@Test
	void establishOpenIDAuthnModeForSecret_shouldReturnSecretBasicForMatchingProvider() throws Exception
	{
		OIDCProviderMetadata meta = mockProviderMetaWithMethods(
				List.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));
		OAuth2Verificator verificator = mockVerificator();

		ClientAuthnMode mode = verificator.establishOpenIDAuthnModeForSecret(meta,
				providerCfgWithSecret("id", "secret"));

		assertThat(mode).isEqualTo(ClientAuthnMode.secretBasic);
	}

	@Test
	void establishOpenIDAuthnModeForSecret_shouldReturnSecretPostForMatchingProvider() throws Exception
	{
		OIDCProviderMetadata meta = mockProviderMetaWithMethods(
				List.of(ClientAuthenticationMethod.CLIENT_SECRET_POST));
		OAuth2Verificator verificator = mockVerificator();

		ClientAuthnMode mode = verificator.establishOpenIDAuthnModeForSecret(meta,
				providerCfgWithSecret("id", "secret"));

		assertThat(mode).isEqualTo(ClientAuthnMode.secretPost);
	}

	@Test
	void establishOpenIDAuthnModeForSecret_shouldUseConfiguredModeOverAutoDetect() throws Exception
	{
		OIDCProviderMetadata meta = mockProviderMetaWithMethods(
				List.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));
		OAuthProviderConfiguration cfg = OAuthProviderConfiguration.builder()
				.withClientId("id")
				.withClientSecret("secret")
				.withClientAuthnMethod(ClientAuthnMethod.client_secret)
				.withClientAuthnMode(Optional.of(ClientAuthnMode.secretPost))
				.build();
		OAuth2Verificator verificator = mockVerificator();

		ClientAuthnMode mode = verificator.establishOpenIDAuthnModeForSecret(meta, cfg);

		assertThat(mode).isEqualTo(ClientAuthnMode.secretPost);
	}

	@Test
	void establishOpenIDAuthnModeForSecret_shouldThrowWhenNoSupportedMethodFound()
	{
		OIDCProviderMetadata meta = mockProviderMetaWithMethods(
				List.of(ClientAuthenticationMethod.PRIVATE_KEY_JWT));
		OAuth2Verificator verificator = mockVerificator();

		assertThatThrownBy(() -> verificator.establishOpenIDAuthnModeForSecret(meta,
				providerCfgWithSecret("id", "secret")))
				.isInstanceOf(AuthenticationException.class);
	}

	private static OAuthProviderConfiguration providerCfgWithPrivateKeyJwt(String clientId, String credName)
	{
		return OAuthProviderConfiguration.builder()
				.withClientId(clientId)
				.withClientAuthnMethod(ClientAuthnMethod.private_key_jwt)
				.withClientCredential(credName)
				.build();
	}

	private static OAuthProviderConfiguration providerCfgWithSecret(String clientId, String secret)
	{
		return OAuthProviderConfiguration.builder()
				.withClientId(clientId)
				.withClientSecret(secret)
				.withClientAuthnMethod(ClientAuthnMethod.client_secret)
				.build();
	}

	private static PKIManagement pkiManWith(X509Credential credential) throws Exception
	{
		PKIManagement pki = mock(PKIManagement.class);
		when(pki.getCredential(anyString())).thenReturn(credential);
		return pki;
	}

	private static X509Credential rsaCredential() throws Exception
	{
		return new KeystoreCredential(RSA_KEYSTORE,
				RSA_KEYSTORE_PASS.toCharArray(), RSA_KEYSTORE_PASS.toCharArray(), null, "pkcs12");
	}

	private static X509Credential ecCredential() throws Exception
	{
		return new KeystoreCredential(EC_KEYSTORE,
				EC_KEYSTORE_PASS.toCharArray(), EC_KEYSTORE_PASS.toCharArray(), null, "pkcs12");
	}

	private static OIDCProviderMetadata mockProviderMetaWithMethods(
			List<ClientAuthenticationMethod> methods)
	{
		OIDCProviderMetadata meta = mock(OIDCProviderMetadata.class);
		when(meta.getTokenEndpointAuthMethods()).thenReturn(methods);
		return meta;
	}

	private static OAuth2Verificator mockVerificator()
	{
		return new OAuth2VerificatorTestBuilder().build();
	}
}
