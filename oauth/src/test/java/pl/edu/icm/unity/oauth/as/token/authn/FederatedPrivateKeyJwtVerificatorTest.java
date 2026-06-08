/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;

class FederatedPrivateKeyJwtVerificatorTest
{
	private static final String CLIENT_ID = "https://client.example.com";
	private static final String TRUST_ANCHOR_ID = "https://anchor.example.com";
	private static final URI TOKEN_URI = URI.create("https://example.com/token");

	private IdentityResolver identityResolver;
	private PKIManagement pkiManagement;

	@BeforeEach
	void setUp()
	{
		identityResolver = mock(IdentityResolver.class);
		pkiManagement = mock(PKIManagement.class);
	}

	private String buildConfig(String trustAnchorId, String anchorJwks)
	{
		ObjectNode root = JsonUtil.parse("{}");
		if (trustAnchorId != null)
			root.put("federationTrustAnchorId", trustAnchorId);
		if (anchorJwks != null)
			root.put("federationTrustAnchorJwks", anchorJwks);
		return JsonUtil.serialize(root);
	}

	@Test
	void shouldFailWhenTrustAnchorNotConfigured()
	{
		var verificator = new FederatedPrivateKeyJwtVerificator(pkiManagement);
		verificator.setIdentityResolver(identityResolver);

		AuthenticationResult result = verificator.verifyClientAssertion("any-jwt", TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailForMalformedJwt() throws Exception
	{
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		var verificator = new FederatedPrivateKeyJwtVerificator(pkiManagement);
		verificator.setIdentityResolver(identityResolver);
		verificator.setSerializedConfiguration(
				buildConfig(TRUST_ANCHOR_ID, new JWKSet(anchorKey.toPublicJWK()).toString()));

		AuthenticationResult result = verificator.verifyClientAssertion("not-a-jwt", TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenClientEntityNotFound() throws Exception
	{
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		var clientKey = new RSAKeyGenerator(2048).keyID("client-key").generate();

		var verificator = new FederatedPrivateKeyJwtVerificator(pkiManagement)
		{
			@Override
			JWKSet resolveJwksFromFederation(String cid) throws Exception
			{
				return new JWKSet(clientKey.toPublicJWK());
			}
		};
		verificator.setIdentityResolver(identityResolver);
		verificator.setSerializedConfiguration(
				buildConfig(TRUST_ANCHOR_ID, new JWKSet(anchorKey.toPublicJWK()).toString()));

		when(identityResolver.resolveIdentity(any(), any(), isNull()))
				.thenThrow(new IllegalArgumentException("Not found"));

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(clientKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("client-key").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldSucceedWhenFederationReturnsMatchingKey() throws Exception
	{
		var clientKey = new RSAKeyGenerator(2048).keyID("client-key").generate();
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();

		var verificator = new FederatedPrivateKeyJwtVerificator(pkiManagement)
		{
			@Override
			JWKSet resolveJwksFromFederation(String cid) throws Exception
			{
				return new JWKSet(clientKey.toPublicJWK());
			}
		};
		verificator.setIdentityResolver(identityResolver);
		verificator.setSerializedConfiguration(
				buildConfig(TRUST_ANCHOR_ID, new JWKSet(anchorKey.toPublicJWK()).toString()));

		EntityWithCredential entity = new EntityWithCredential("cred", null, 42L);
		when(identityResolver.resolveIdentity(any(), any(), isNull())).thenReturn(entity);

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(clientKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("client-key").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.success);
	}

	@Test
	void shouldFailWhenSignatureDoesNotMatchFederationKey() throws Exception
	{
		var clientKey = new RSAKeyGenerator(2048).keyID("client-key").generate();
		var wrongKey = new RSAKeyGenerator(2048).keyID("wrong").generate();
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();

		var verificator = new FederatedPrivateKeyJwtVerificator(pkiManagement)
		{
			@Override
			JWKSet resolveJwksFromFederation(String cid) throws Exception
			{
				return new JWKSet(clientKey.toPublicJWK());
			}
		};
		verificator.setIdentityResolver(identityResolver);
		verificator.setSerializedConfiguration(
				buildConfig(TRUST_ANCHOR_ID, new JWKSet(anchorKey.toPublicJWK()).toString()));

		EntityWithCredential entity = new EntityWithCredential("cred", null, 42L);
		when(identityResolver.resolveIdentity(any(), any(), isNull())).thenReturn(entity);

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(wrongKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("wrong").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldRoundTripSerializedConfiguration() throws Exception
	{
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		String anchorJwks = new JWKSet(anchorKey.toPublicJWK()).toString();

		var verificator = new FederatedPrivateKeyJwtVerificator(pkiManagement);
		verificator.setSerializedConfiguration(buildConfig(TRUST_ANCHOR_ID, anchorJwks));

		String serialized = verificator.getSerializedConfiguration();

		assertThat(serialized).contains("\"federationTrustAnchorId\":\"" + TRUST_ANCHOR_ID + "\"");
		assertThat(serialized).contains("federationTrustAnchorJwks");
	}

	@Test
	void shouldThrowForInvalidFederationJwksInConfig()
	{
		var verificator = new FederatedPrivateKeyJwtVerificator(pkiManagement);
		assertThatThrownBy(() ->
				verificator.setSerializedConfiguration(buildConfig(null, "not-valid-jwks")))
				.isInstanceOf(InternalException.class)
				.hasMessageContaining("Invalid federation trust anchor JWKS");
	}

	private String buildAndSignJwt(String clientId, URI audience, com.nimbusds.jose.JWSSigner signer,
			JWSHeader header) throws Exception
	{
		JWTClaimsSet claims = new JWTClaimsSet.Builder()
				.subject(clientId)
				.issuer(clientId)
				.audience(audience.toString())
				.expirationTime(new Date(System.currentTimeMillis() + 60_000))
				.build();
		SignedJWT jwt = new SignedJWT(header, claims);
		jwt.sign(signer);
		return jwt.serialize();
	}
}
