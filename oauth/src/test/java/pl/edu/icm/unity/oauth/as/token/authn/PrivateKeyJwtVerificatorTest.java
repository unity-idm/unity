/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.authn.LocalCredentialState;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.oauth.as.OAuthASFederationConfig;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;

class PrivateKeyJwtVerificatorTest
{
	private static final String CLIENT_ID = "my-client";
	private static final String CREDENTIAL_NAME = "test-credential";
	private static final String CLIENTS_GROUP = "/oauth-clients";
	private static final URI TOKEN_URI = URI.create("https://example.com/token");

	private IdentityResolver identityResolver;
	private CredentialHelper credentialHelper;
	private OAuthEndpointsCoordinator coordinator;
	private AttributesManagement attributesManagement;
	private PrivateKeyJwtVerificator verificator;

	@BeforeEach
	void setUp()
	{
		identityResolver = mock(IdentityResolver.class);
		credentialHelper = mock(CredentialHelper.class);
		coordinator = mock(OAuthEndpointsCoordinator.class);
		attributesManagement = mock(AttributesManagement.class);
		OAuthASFederationConfig federationConfig = new OAuthASFederationConfig(
				false, null, null, null, null, CLIENTS_GROUP);
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(federationConfig));
		verificator = new PrivateKeyJwtVerificator(credentialHelper, coordinator, attributesManagement);
		verificator.setIdentityResolver(identityResolver);
		verificator.setCredentialName(CREDENTIAL_NAME);
	}

	private AttributeExt authnMethodAttr(String value)
	{
		return new AttributeExt(
				new Attribute("sys:oauth:clientAuthnMethod", "enumeration", CLIENTS_GROUP, List.of(value)),
				true);
	}

	@Test
	void shouldSucceedForValidRsaAssertion() throws Exception
	{
		var rsaKey = new RSAKeyGenerator(2048).keyID("rsa-1").generate();
		var jwkSet = new JWKSet(rsaKey.toPublicJWK());
		stubIdentityResolver(jwkSet.toString());

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(rsaKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("rsa-1").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.success);
	}

	@Test
	void shouldSucceedForValidEcAssertion() throws Exception
	{
		ECKey ecKey = new ECKeyGenerator(Curve.P_256).keyID("ec-1").generate();
		var jwkSet = new JWKSet(ecKey.toPublicJWK());
		stubIdentityResolver(jwkSet.toString());

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new ECDSASigner(ecKey),
				new JWSHeader.Builder(JWSAlgorithm.ES256).keyID("ec-1").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.success);
	}

	@Test
	void shouldFailForMalformedJwt()
	{
		AuthenticationResult result = verificator.verifyClientAssertion("not-a-jwt", TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenSubjectClaimMissing() throws Exception
	{
		var rsaKey = new RSAKeyGenerator(2048).keyID("k1").generate();
		Date now = new Date();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.issuer(CLIENT_ID)
				.audience(TOKEN_URI.toString())
				.issueTime(now)
				.expirationTime(new Date(now.getTime() + 60_000))
				.jwtID(UUID.randomUUID().toString())
				.build();
		SignedJWT jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(), claims);
		jwt.sign(new RSASSASigner(rsaKey));

		AuthenticationResult result = verificator.verifyClientAssertion(jwt.serialize(), TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailForExpiredJwt() throws Exception
	{
		var rsaKey = new RSAKeyGenerator(2048).keyID("k1").generate();
		var jwkSet = new JWKSet(rsaKey.toPublicJWK());
		stubIdentityResolver(jwkSet.toString());

		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(CLIENT_ID)
				.issuer(CLIENT_ID)
				.audience(TOKEN_URI.toString())
				.issueTime(new Date(System.currentTimeMillis() - 2000))
				.expirationTime(new Date(System.currentTimeMillis() - 1000))
				.jwtID(UUID.randomUUID().toString())
				.build();
		SignedJWT jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(), claims);
		jwt.sign(new RSASSASigner(rsaKey));

		AuthenticationResult result = verificator.verifyClientAssertion(jwt.serialize(), TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenAudienceDoesNotMatchTokenEndpoint() throws Exception
	{
		var rsaKey = new RSAKeyGenerator(2048).keyID("k1").generate();
		var jwkSet = new JWKSet(rsaKey.toPublicJWK());
		stubIdentityResolver(jwkSet.toString());

		Date now = new Date();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(CLIENT_ID)
				.issuer(CLIENT_ID)
				.audience("https://other.example.com/token")
				.issueTime(now)
				.expirationTime(new Date(now.getTime() + 60_000))
				.jwtID(UUID.randomUUID().toString())
				.build();
		SignedJWT jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(), claims);
		jwt.sign(new RSASSASigner(rsaKey));

		AuthenticationResult result = verificator.verifyClientAssertion(jwt.serialize(), TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenIssuerDoesNotMatchClientId() throws Exception
	{
		var rsaKey = new RSAKeyGenerator(2048).keyID("k1").generate();
		var jwkSet = new JWKSet(rsaKey.toPublicJWK());
		stubIdentityResolver(jwkSet.toString());

		Date now = new Date();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(CLIENT_ID)
				.issuer("other-client")
				.audience(TOKEN_URI.toString())
				.issueTime(now)
				.expirationTime(new Date(now.getTime() + 60_000))
				.jwtID(UUID.randomUUID().toString())
				.build();
		SignedJWT jwt = new SignedJWT(
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(), claims);
		jwt.sign(new RSASSASigner(rsaKey));

		AuthenticationResult result = verificator.verifyClientAssertion(jwt.serialize(), TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenClientNotFound() throws Exception
	{
		var rsaKey = new RSAKeyGenerator(2048).keyID("k1").generate();
		when(identityResolver.resolveIdentity(eq(CLIENT_ID), any(), eq(CREDENTIAL_NAME)))
				.thenThrow(new IllegalArgumentException("Entity not found"));

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(rsaKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenClientAuthnMethodIsNotPrivateKeyJwt() throws Exception
	{
		var rsaKey = new RSAKeyGenerator(2048).keyID("rsa-1").generate();
		var jwkSet = new JWKSet(rsaKey.toPublicJWK());
		EntityWithCredential entity = new EntityWithCredential(CREDENTIAL_NAME, jwkSet.toString(), 42L);
		when(identityResolver.resolveIdentity(eq(CLIENT_ID), any(), eq(CREDENTIAL_NAME))).thenReturn(entity);
		when(attributesManagement.getAttributes(any(), eq(CLIENTS_GROUP), any()))
				.thenReturn(List.of(authnMethodAttr("client_secret")));

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(rsaKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("rsa-1").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenNoJwksStored() throws Exception
	{
		var rsaKey = new RSAKeyGenerator(2048).keyID("k1").generate();
		stubIdentityResolver(null);

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(rsaKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenSignatureDoesNotMatchStoredKey() throws Exception
	{
		var keyInStore = new RSAKeyGenerator(2048).keyID("k1").generate();
		var otherKey = new RSAKeyGenerator(2048).keyID("k1").generate();
		var jwkSet = new JWKSet(keyInStore.toPublicJWK());
		stubIdentityResolver(jwkSet.toString());

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(otherKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldSelectKeyByKid() throws Exception
	{
		var correctKey = new RSAKeyGenerator(2048).keyID("correct").generate();
		var otherKey = new RSAKeyGenerator(2048).keyID("other").generate();
		var jwkSet = new JWKSet(List.of(otherKey.toPublicJWK(), correctKey.toPublicJWK()));
		stubIdentityResolver(jwkSet.toString());

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(correctKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("correct").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.success);
	}

	@Test
	void shouldRoundTripSerializedConfiguration() throws Exception
	{
		verificator.setSerializedConfiguration("{\"credentialName\":\"cred\"}");

		String serialized = verificator.getSerializedConfiguration();

		assertThat(serialized).contains("\"credentialName\":\"cred\"");
	}

	@Test
	void checkCredentialState_notSet_whenBlank()
	{
		assertThat(verificator.checkCredentialState("").getState())
				.isEqualTo(LocalCredentialState.notSet);
		assertThat(verificator.checkCredentialState(null).getState())
				.isEqualTo(LocalCredentialState.notSet);
	}

	@Test
	void checkCredentialState_correct_whenNonBlank()
	{
		assertThat(verificator.checkCredentialState("{\"keys\":[]}").getState())
				.isEqualTo(LocalCredentialState.correct);
	}

	@Test
	void prepareCredential_acceptsValidJwks() throws Exception
	{
		ECKey key = new ECKeyGenerator(Curve.P_256).keyID("k1").generate();
		String jwks = new JWKSet(key.toPublicJWK()).toString();

		String stored = verificator.prepareCredential(jwks, null, false);

		assertThat(stored).isEqualTo(jwks);
	}

	@Test
	void prepareCredential_rejectsInvalidJwks()
	{
		org.assertj.core.api.Assertions.assertThatThrownBy(
				() -> verificator.prepareCredential("invalid-json", null, false))
				.isInstanceOf(InternalException.class)
				.hasMessageContaining("Invalid JWK Set");
	}

	@Test
	void prepareCredential_acceptsBlank() throws Exception
	{
		assertThat(verificator.prepareCredential("", null, false)).isEmpty();
		assertThat(verificator.prepareCredential(null, null, false)).isEmpty();
	}

	private void stubIdentityResolver(String credentialValue) throws Exception
	{
		EntityWithCredential entity = new EntityWithCredential(CREDENTIAL_NAME, credentialValue, 42L);
		when(identityResolver.resolveIdentity(eq(CLIENT_ID), any(), eq(CREDENTIAL_NAME)))
				.thenReturn(entity);
		when(attributesManagement.getAttributes(any(), eq(CLIENTS_GROUP), any()))
				.thenReturn(List.of(authnMethodAttr("private_key_jwt")));
	}

	private String buildAndSignJwt(String clientId, URI audience, com.nimbusds.jose.JWSSigner signer,
			JWSHeader header) throws Exception
	{
		Date now = new Date();
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(clientId)
				.issuer(clientId)
				.audience(audience.toString())
				.issueTime(now)
				.expirationTime(new Date(now.getTime() + 60_000))
				.jwtID(UUID.randomUUID().toString())
				.build();
		SignedJWT jwt = new SignedJWT(header, claims);
		jwt.sign(signer);
		return jwt.serialize();
	}
}
