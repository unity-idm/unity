/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientInformation;
import com.nimbusds.openid.connect.sdk.rp.OIDCClientMetadata;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.oauth.as.OAuthASFederationConfig;
import pl.edu.icm.unity.oauth.as.OAuthEndpointsCoordinator;

class FederatedPrivateKeyJwtVerificatorTest
{
	private static final String CLIENT_ID = "https://client.example.com";
	private static final String TRUST_ANCHOR_ID = "https://anchor.example.com";
	private static final String CLIENTS_GROUP = "/oauth-clients";
	private static final URI TOKEN_URI = URI.create("https://example.com/token");

	private IdentityResolver identityResolver;
	private OAuthEndpointsCoordinator coordinator;
	private AttributesManagement attributesManagement;

	@BeforeEach
	void setUp()
	{
		identityResolver = mock(IdentityResolver.class);
		coordinator = mock(OAuthEndpointsCoordinator.class);
		attributesManagement = mock(AttributesManagement.class);
	}

	private OAuthASFederationConfig configWithAnchor(JWKSet anchorJwks)
	{
		return new OAuthASFederationConfig(true, TRUST_ANCHOR_ID, anchorJwks, null, null, CLIENTS_GROUP);
	}

	private FederatedPrivateKeyJwtVerificator verificator()
	{
		var v = new FederatedPrivateKeyJwtVerificator(coordinator, attributesManagement);
		v.setIdentityResolver(identityResolver);
		return v;
	}

	private AttributeExt authnMethodAttr(String value)
	{
		return new AttributeExt(
				new Attribute("sys:oauth:clientAuthnMethod", "enumeration", CLIENTS_GROUP, List.of(value)),
				true);
	}

	@Test
	void shouldFailWhenNoConfigRegistered()
	{
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.empty());
		var verificator = verificator();

		AuthenticationResult result = verificator.verifyClientAssertion("any-jwt", TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenFederationMembershipDisabled() throws Exception
	{
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		OAuthASFederationConfig config = new OAuthASFederationConfig(
				false, TRUST_ANCHOR_ID, new JWKSet(anchorKey.toPublicJWK()), null, null, CLIENTS_GROUP);
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(config));
		var verificator = verificator();

		AuthenticationResult result = verificator.verifyClientAssertion("any-jwt", TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailForMalformedJwt() throws Exception
	{
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		when(coordinator.getFederationConfig(TOKEN_URI.toString()))
				.thenReturn(Optional.of(configWithAnchor(new JWKSet(anchorKey.toPublicJWK()))));
		var verificator = verificator();

		AuthenticationResult result = verificator.verifyClientAssertion("not-a-jwt", TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenClientEntityNotFound() throws Exception
	{
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		var clientKey = new RSAKeyGenerator(2048).keyID("client-key").generate();
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(anchorKey.toPublicJWK()));
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(config));

		var verificator = new FederatedPrivateKeyJwtVerificator(coordinator, attributesManagement)
		{
			@Override
			JWKSet resolveJwksFromFederation(String cid, OAuthASFederationConfig cfg) throws Exception
			{
				return new JWKSet(clientKey.toPublicJWK());
			}
		};
		verificator.setIdentityResolver(identityResolver);
		when(identityResolver.resolveIdentity(any(), any(), isNull()))
				.thenThrow(new IllegalArgumentException("Not found"));

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(clientKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("client-key").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldFailWhenClientAuthnMethodIsNotPrivateKeyJwt() throws Exception
	{
		var clientKey = new RSAKeyGenerator(2048).keyID("client-key").generate();
		var anchorKey = new RSAKeyGenerator(2048).keyID("anchor").generate();
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(anchorKey.toPublicJWK()));
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(config));

		var verificator = new FederatedPrivateKeyJwtVerificator(coordinator, attributesManagement)
		{
			@Override
			JWKSet resolveJwksFromFederation(String cid, OAuthASFederationConfig cfg) throws Exception
			{
				return new JWKSet(clientKey.toPublicJWK());
			}
		};
		verificator.setIdentityResolver(identityResolver);
		EntityWithCredential entity = new EntityWithCredential("cred", null, 42L);
		when(identityResolver.resolveIdentity(any(), any(), isNull())).thenReturn(entity);
		when(attributesManagement.getAttributes(any(), eq(CLIENTS_GROUP), any()))
				.thenReturn(List.of(authnMethodAttr("client_secret")));

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
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(anchorKey.toPublicJWK()));
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(config));

		var verificator = new FederatedPrivateKeyJwtVerificator(coordinator, attributesManagement)
		{
			@Override
			JWKSet resolveJwksFromFederation(String cid, OAuthASFederationConfig cfg) throws Exception
			{
				return new JWKSet(clientKey.toPublicJWK());
			}
		};
		verificator.setIdentityResolver(identityResolver);
		EntityWithCredential entity = new EntityWithCredential("cred", null, 42L);
		when(identityResolver.resolveIdentity(any(), any(), isNull())).thenReturn(entity);
		when(attributesManagement.getAttributes(any(), eq(CLIENTS_GROUP), any()))
				.thenReturn(List.of(authnMethodAttr("private_key_jwt")));

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
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(anchorKey.toPublicJWK()));
		when(coordinator.getFederationConfig(TOKEN_URI.toString())).thenReturn(Optional.of(config));

		var verificator = new FederatedPrivateKeyJwtVerificator(coordinator, attributesManagement)
		{
			@Override
			JWKSet resolveJwksFromFederation(String cid, OAuthASFederationConfig cfg) throws Exception
			{
				return new JWKSet(clientKey.toPublicJWK());
			}
		};
		verificator.setIdentityResolver(identityResolver);
		EntityWithCredential entity = new EntityWithCredential("cred", null, 42L);
		when(identityResolver.resolveIdentity(any(), any(), isNull())).thenReturn(entity);
		when(attributesManagement.getAttributes(any(), eq(CLIENTS_GROUP), any()))
				.thenReturn(List.of(authnMethodAttr("private_key_jwt")));

		String jwt = buildAndSignJwt(CLIENT_ID, TOKEN_URI, new RSASSASigner(wrongKey),
				new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("wrong").build());

		AuthenticationResult result = verificator.verifyClientAssertion(jwt, TOKEN_URI);

		assertThat(result.getStatus()).isEqualTo(Status.deny);
	}

	@Test
	void shouldReturnEmptySerializedConfiguration()
	{
		var verificator = new FederatedPrivateKeyJwtVerificator(coordinator, attributesManagement);
		assertThat(verificator.getSerializedConfiguration()).isEqualTo("{}");
	}

	@Test
	void shouldExtractRpJwksFromChainRpMetadataNotTopLevelJwks() throws Exception
	{
		RSAKey federationKey = new RSAKeyGenerator(2048).keyID("fed-key").generate();
		RSAKey authKey = new RSAKeyGenerator(2048).keyID("auth-key").generate();

		TrustChain chain = buildChainWithRpJwks(CLIENT_ID, federationKey, authKey);

		JWKSet result = FederatedPrivateKeyJwtVerificator.extractRpJwksFromChain(chain, CLIENT_ID);

		assertThat(result.getKeyByKeyId("auth-key")).isNotNull();
		assertThat(result.getKeyByKeyId("fed-key")).isNull();
	}

	@Test
	void shouldThrowWhenChainHasNoRpMetadata() throws Exception
	{
		RSAKey federationKey = new RSAKeyGenerator(2048).keyID("fed-key").generate();
		TrustChain chain = buildChainWithoutRpMetadata(CLIENT_ID, federationKey);

		assertThatThrownBy(() -> FederatedPrivateKeyJwtVerificator.extractRpJwksFromChain(chain, CLIENT_ID))
				.hasMessageContaining("No openid_relying_party metadata");
	}

	@Test
	void shouldReturnCachedChainWithoutCallingResolveAgain() throws Exception
	{
		RSAKey federationKey = new RSAKeyGenerator(2048).keyID("fed-key").generate();
		RSAKey authKey = new RSAKeyGenerator(2048).keyID("auth-key").generate();
		TrustChain chain = buildChainWithRpJwks(CLIENT_ID, federationKey, authKey);
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(federationKey.toPublicJWK()));
		int[] resolveCount = {0};

		var verificator = new FederatedPrivateKeyJwtVerificator(coordinator, attributesManagement)
		{
			@Override
			TrustChain resolveChain(String clientId, OAuthASFederationConfig cfg) throws Exception
			{
				resolveCount[0]++;
				return chain;
			}
		};

		verificator.resolveJwksFromFederation(CLIENT_ID, config);
		verificator.resolveJwksFromFederation(CLIENT_ID, config);

		assertThat(resolveCount[0]).isEqualTo(1);
	}

	@Test
	void shouldResolveAgainWhenCacheEntryExpired() throws Exception
	{
		RSAKey federationKey = new RSAKeyGenerator(2048).keyID("fed-key").generate();
		RSAKey authKey = new RSAKeyGenerator(2048).keyID("auth-key").generate();
		TrustChain chain = buildChainWithRpJwks(CLIENT_ID, federationKey, authKey);
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(federationKey.toPublicJWK()));
		int[] resolveCount = {0};

		var verificator = new FederatedPrivateKeyJwtVerificator(coordinator, attributesManagement)
		{
			@Override
			TrustChain resolveChain(String clientId, OAuthASFederationConfig cfg) throws Exception
			{
				resolveCount[0]++;
				return chain;
			}
		};
		verificator.chainCache.put(CLIENT_ID,
				new FederatedPrivateKeyJwtVerificator.CachedChain(chain, Instant.now().minusSeconds(1)));

		verificator.resolveJwksFromFederation(CLIENT_ID, config);

		assertThat(resolveCount[0]).isEqualTo(1);
	}

	@Test
	void shouldEvictExpiredEntriesOnNextResolve() throws Exception
	{
		RSAKey federationKey = new RSAKeyGenerator(2048).keyID("fed-key").generate();
		RSAKey authKey = new RSAKeyGenerator(2048).keyID("auth-key").generate();
		TrustChain chain = buildChainWithRpJwks(CLIENT_ID, federationKey, authKey);
		OAuthASFederationConfig config = configWithAnchor(new JWKSet(federationKey.toPublicJWK()));

		var verificator = new FederatedPrivateKeyJwtVerificator(coordinator, attributesManagement)
		{
			@Override
			TrustChain resolveChain(String clientId, OAuthASFederationConfig cfg) { return chain; }
		};
		verificator.chainCache.put("stale-client-1",
				new FederatedPrivateKeyJwtVerificator.CachedChain(chain, Instant.now().minusSeconds(1)));
		verificator.chainCache.put("stale-client-2",
				new FederatedPrivateKeyJwtVerificator.CachedChain(chain, Instant.now().minusSeconds(1)));

		verificator.resolveJwksFromFederation(CLIENT_ID, config);

		assertThat(verificator.chainCache).doesNotContainKey("stale-client-1");
		assertThat(verificator.chainCache).doesNotContainKey("stale-client-2");
	}

	@Test
	void shouldClearCacheOnReconfiguration() throws Exception
	{
		RSAKey federationKey = new RSAKeyGenerator(2048).keyID("fed-key").generate();
		RSAKey authKey = new RSAKeyGenerator(2048).keyID("auth-key").generate();
		TrustChain chain = buildChainWithRpJwks(CLIENT_ID, federationKey, authKey);

		var verificator = new FederatedPrivateKeyJwtVerificator(coordinator, attributesManagement);
		verificator.setSerializedConfiguration("{}");
		verificator.chainCache.put(CLIENT_ID,
				new FederatedPrivateKeyJwtVerificator.CachedChain(chain, Instant.now().plusSeconds(3600)));

		verificator.setSerializedConfiguration("{}");

		assertThat(verificator.chainCache).isEmpty();
	}

	private TrustChain buildChainWithRpJwks(String clientId, RSAKey federationKey, RSAKey authKey)
			throws Exception
	{
		EntityID leafId = new EntityID(clientId);
		EntityID anchorId = new EntityID(TRUST_ANCHOR_ID);
		Date now = new Date();
		Date exp = Date.from(Instant.now().plusSeconds(3600));
		JWKSet federationJwks = new JWKSet(federationKey.toPublicJWK());

		EntityStatementClaimsSet leafClaims = new EntityStatementClaimsSet(leafId, leafId, now, exp, federationJwks);
		OIDCClientMetadata rpMeta = new OIDCClientMetadata();
		rpMeta.setJWKSet(new JWKSet(authKey.toPublicJWK()));
		leafClaims.setRPInformation(new OIDCClientInformation(new ClientID(clientId), rpMeta));
		EntityStatement leafStatement = EntityStatement.sign(leafClaims, federationKey);

		EntityStatementClaimsSet anchorClaims = new EntityStatementClaimsSet(anchorId, leafId, now, exp, federationJwks);
		EntityStatement anchorStatement = EntityStatement.sign(anchorClaims, federationKey);

		return new TrustChain(leafStatement, List.of(anchorStatement));
	}

	private TrustChain buildChainWithoutRpMetadata(String clientId, RSAKey federationKey) throws Exception
	{
		EntityID leafId = new EntityID(clientId);
		EntityID anchorId = new EntityID(TRUST_ANCHOR_ID);
		Date now = new Date();
		Date exp = Date.from(Instant.now().plusSeconds(3600));
		JWKSet federationJwks = new JWKSet(federationKey.toPublicJWK());

		EntityStatementClaimsSet leafClaims = new EntityStatementClaimsSet(leafId, leafId, now, exp, federationJwks);
		EntityStatement leafStatement = EntityStatement.sign(leafClaims, federationKey);

		EntityStatementClaimsSet anchorClaims = new EntityStatementClaimsSet(anchorId, leafId, now, exp, federationJwks);
		EntityStatement anchorStatement = EntityStatement.sign(anchorClaims, federationKey);

		return new TrustChain(leafStatement, List.of(anchorStatement));
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
