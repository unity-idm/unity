/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;
import pl.edu.icm.unity.oauth.as.federation.FederatedOAuthClientService;
import pl.edu.icm.unity.oauth.as.federation.FederatedOAuthClientService.FederatedClientResolution;
import pl.edu.icm.unity.oauth.as.federation.OAuthASFederationConfig;

class FederatedPrivateKeyJwtVerificatorTest
{
	private static final String CLIENT_ID = "https://client.example.com";
	private static final String TRUST_ANCHOR_ID = "https://anchor.example.com";
	private static final String CLIENTS_GROUP = "/oauth-clients";
	private static final URI TOKEN_URI = URI.create("https://example.com/token");

	private OAuthEndpointsCoordinator coordinator;
	private FederatedOAuthClientService federationClientService;

	@BeforeEach
	void setUp()
	{
		coordinator = mock(OAuthEndpointsCoordinator.class);
		federationClientService = mock(FederatedOAuthClientService.class);
	}

	private OAuthASFederationConfig configWithAnchor(JWKSet anchorJwks)
	{
		return new OAuthASFederationConfig(true, TRUST_ANCHOR_ID, anchorJwks, null, null, CLIENTS_GROUP);
	}

	private FederatedPrivateKeyJwtVerificator verificator()
	{
		return new FederatedPrivateKeyJwtVerificator(coordinator, federationClientService);
	}

	@Test
	void shouldFailWhenNoConfigRegistered()
	{
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.empty());

		AuthenticationResult result = verificator().verifyClientAssertion("any-jwt", TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenFederationMembershipDisabled() throws Exception
	{
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		OAuthASFederationConfig config = new OAuthASFederationConfig(
				false, TRUST_ANCHOR_ID, new JWKSet(anchorKey.toPublicJWK()), null, null, CLIENTS_GROUP);
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(config));

		AuthenticationResult result = verificator().verifyClientAssertion("any-jwt", TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailForMalformedJwt() throws Exception
	{
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		when(coordinator.getFederationConfig(TOKEN_URI.toString()))
				.thenReturn(Optional.of(configWithAnchor(new JWKSet(anchorKey.toPublicJWK()))));

		AuthenticationResult result = verificator().verifyClientAssertion("not-a-jwt", TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenChainResolutionFails() throws Exception
	{
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		var clientKey = new RSAKeyGenerator(2048).keyID("client-key").generate();
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(anchorKey.toPublicJWK()));
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(config));
		when(federationClientService.resolveAndRegister(eq(CLIENT_ID), any()))
				.thenThrow(new Exception("Trust chain resolution failed"));

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(clientKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("client-key").build());

		AuthenticationResult result = verificator().verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenRegistrationFails() throws Exception
	{
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		var clientKey = new RSAKeyGenerator(2048).keyID("client-key").generate();
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(anchorKey.toPublicJWK()));
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(config));
		when(federationClientService.resolveAndRegister(eq(CLIENT_ID), any()))
				.thenThrow(new Exception("DB error"));

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(clientKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("client-key").build());

		AuthenticationResult result = verificator().verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldSucceedWhenFederationReturnsMatchingKey() throws Exception
	{
		var clientKey = new RSAKeyGenerator(2048).keyID("client-key").generate();
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(anchorKey.toPublicJWK()));
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(config));
		when(federationClientService.resolveAndRegister(eq(CLIENT_ID), any()))
				.thenReturn(new FederatedClientResolution(42L, new JWKSet(clientKey.toPublicJWK())));

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(clientKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("client-key").build());

		AuthenticationResult result = verificator().verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.success);
	}

	@Test
	void shouldFailWhenSignatureDoesNotMatchFederationKey() throws Exception
	{
		var clientKey = new RSAKeyGenerator(2048).keyID("client-key").generate();
		var wrongKey = new RSAKeyGenerator(2048).keyID("wrong").generate();
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(anchorKey.toPublicJWK()));
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(config));
		when(federationClientService.resolveAndRegister(eq(CLIENT_ID), any()))
				.thenReturn(new FederatedClientResolution(42L, new JWKSet(clientKey.toPublicJWK())));

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(wrongKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("wrong").build());

		AuthenticationResult result = verificator().verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldReturnEmptySerializedConfiguration()
	{
		assertThat(verificator().getSerializedConfiguration()).isEqualTo("{}");
	}

	@Test
	void shouldClearChainCacheOnReconfiguration()
	{
		verificator().setSerializedConfiguration("{}");

		verify(federationClientService).invalidateChainCache();
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
