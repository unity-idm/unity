/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.registration.ClientRegistrationType;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;

import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import pl.edu.icm.unity.oauth.as.token.CredentialJwkConverter;
import pl.edu.icm.unity.oauth.as.token.KeyIdExtractor;

class OAuthFederationEntityStatementGeneratorTest
{
	private static final String ENTITY_ID = "https://rp.example.com";
	private static final String CALLBACK_URL = "https://rp.example.com/callback";
	private static final String SUPERIOR_ENTITY_ID = "https://federation-anchor.example.com";
	private static final String RSA_KEYSTORE = "src/test/resources/pki/demoKeystore.p12";
	private static final String RSA_KEYSTORE_PASS = "the!unity";
	private static final String EC_KEYSTORE = "src/test/resources/demoECKey.p12";
	private static final String EC_KEYSTORE_PASS = "123456";

	@Test
	void shouldSetEntityIdAsIssuerAndSubject() throws Exception
	{
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, rsaCredential(), null, CALLBACK_URL, null, 3600);

		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);

		EntityStatementClaimsSet claims = statement.getClaimsSet();
		assertThat(claims.getIssuer().getValue()).isEqualTo(ENTITY_ID);
		assertThat(claims.getSubject().getValue()).isEqualTo(ENTITY_ID);
	}

	@Test
	void shouldSetCallbackUrlAsRpRedirectUri() throws Exception
	{
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, rsaCredential(), null, CALLBACK_URL, null, 3600);

		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);

		OIDCClientInformation rpInfo = statement.getClaimsSet().getRPInformation();
		assertThat(rpInfo.getOIDCMetadata().getRedirectionURIs())
				.containsExactly(new URI(CALLBACK_URL));
	}

	@Test
	void shouldSetCodeResponseTypeAndAuthorizationCodeGrantType() throws Exception
	{
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, rsaCredential(), null, CALLBACK_URL, null, 3600);

		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);

		OIDCClientInformation rpInfo = statement.getClaimsSet().getRPInformation();
		assertThat(rpInfo.getOIDCMetadata().getResponseTypes())
				.containsExactlyInAnyOrder(ResponseType.CODE);
		assertThat(rpInfo.getOIDCMetadata().getGrantTypes())
				.containsExactlyInAnyOrder(GrantType.AUTHORIZATION_CODE);
	}

	@Test
	void shouldSetAutomaticClientRegistrationType() throws Exception
	{
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, rsaCredential(), null, CALLBACK_URL, null, 3600);

		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);

		OIDCClientInformation rpInfo = statement.getClaimsSet().getRPInformation();
		assertThat(rpInfo.getOIDCMetadata().getClientRegistrationTypes())
				.containsExactly(ClientRegistrationType.AUTOMATIC);
	}

	@Test
	void shouldSetAuthorityHintsWhenSuperiorEntityIdProvided() throws Exception
	{
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, rsaCredential(), null, CALLBACK_URL, SUPERIOR_ENTITY_ID, 3600);

		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);

		assertThat(statement.getClaimsSet().getAuthorityHints())
				.hasSize(1)
				.first()
				.satisfies(hint -> assertThat(hint.getValue()).isEqualTo(SUPERIOR_ENTITY_ID));
	}

	@Test
	void shouldNotSetAuthorityHintsWhenSuperiorEntityIdIsNull() throws Exception
	{
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, rsaCredential(), null, CALLBACK_URL, null, 3600);

		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);

		assertThat(statement.getClaimsSet().getAuthorityHints()).isNull();
	}

	@Test
	void shouldUseFederationKeyInRpJwksWhenAuthCredentialIsNull() throws Exception
	{
		X509Credential fedCred = rsaCredential();
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, fedCred, null, CALLBACK_URL, null, 3600);

		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);

		String expectedKid = KeyIdExtractor.getKeyId(fedCred.getCertificate());
		JWKSet jwkSet = statement.getClaimsSet().getRPInformation().getOIDCMetadata().getJWKSet();
		assertThat(jwkSet.getKeys()).hasSize(1);
		assertThat(jwkSet.getKeys().get(0).getKeyID()).isEqualTo(expectedKid);
	}

	@Test
	void shouldUseFederationKeyInRpJwksWhenAuthCredentialHasSameKid() throws Exception
	{
		X509Credential fedCred = rsaCredential();
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, fedCred, fedCred, CALLBACK_URL, null, 3600);

		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);

		String expectedKid = KeyIdExtractor.getKeyId(fedCred.getCertificate());
		JWKSet jwkSet = statement.getClaimsSet().getRPInformation().getOIDCMetadata().getJWKSet();
		assertThat(jwkSet.getKeys()).hasSize(1);
		assertThat(jwkSet.getKeys().get(0).getKeyID()).isEqualTo(expectedKid);
	}

	@Test
	void shouldUseAuthKeyInRpJwksWhenItHasDifferentKidThanFederationKey() throws Exception
	{
		X509Credential fedCred = rsaCredential();
		X509Credential authCred = ecCredential();
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, fedCred, authCred, CALLBACK_URL, null, 3600);

		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);

		String expectedKid = KeyIdExtractor.getKeyId(authCred.getCertificate());
		JWKSet jwkSet = statement.getClaimsSet().getRPInformation().getOIDCMetadata().getJWKSet();
		assertThat(jwkSet.getKeys()).hasSize(1);
		assertThat(jwkSet.getKeys().get(0).getKeyID()).isEqualTo(expectedKid);
	}

	@Test
	void shouldSignEntityStatementWithFederationCredential() throws Exception
	{
		X509Credential fedCred = rsaCredential();
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, fedCred, null, CALLBACK_URL, null, 3600);

		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);

		JWKSet federationPublicJwks = new JWKSet(
				CredentialJwkConverter.buildPublicJWK(fedCred.getCertificate()));
		statement.verifySignature(federationPublicJwks);
	}

	@Test
	void shouldSetExpiryFromValiditySeconds() throws Exception
	{
		long validitySeconds = 7200L;
		OAuthFederationEntityStatementConfig config = new OAuthFederationEntityStatementConfig(
				ENTITY_ID, rsaCredential(), null, CALLBACK_URL, null, validitySeconds);

		long before = System.currentTimeMillis();
		EntityStatement statement = OAuthFederationEntityStatementGenerator.generate(config);
		long after = System.currentTimeMillis();

		Date exp = statement.getClaimsSet().getExpirationTime();
		assertThat(exp.getTime()).isBetween(
				before + validitySeconds * 1000 - 1000,
				after + validitySeconds * 1000 + 1000);
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
}
