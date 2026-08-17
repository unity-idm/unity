/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatementClaimsSet;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;

import pl.edu.icm.unity.oauth.client.federation.OAuthFederationLoader.CachedTrustChain;

class OAuthFederationLoaderTest
{
	private static final EntityID TRUST_ANCHOR_ID = new EntityID("https://anchor.example.com");
	private static final EntityID LEAF_ID = new EntityID("https://idp.example.com");
	

	@Test
	void shouldHaveNoExpiredEntriesOnEmptyCache()
	{
		OAuthFederationLoader loader = new OAuthFederationLoader(new ConcurrentHashMap<>());

		assertThat(loader.hasExpiredEntries()).isFalse();
	}

	@Test
	void shouldDetectExpiredCacheEntry() throws Exception
	{
		Map<EntityID, CachedTrustChain> cache = cacheWith(LEAF_ID, buildChain(), Instant.now().minusSeconds(1));
		OAuthFederationLoader loader = new OAuthFederationLoader(cache);

		assertThat(loader.hasExpiredEntries()).isTrue();
	}

	@Test
	void shouldNotDetectExpiredEntryForFreshChain() throws Exception
	{
		Map<EntityID, CachedTrustChain> cache = cacheWith(LEAF_ID, buildChain(), Instant.now().plusSeconds(3600));
		OAuthFederationLoader loader = new OAuthFederationLoader(cache);

		assertThat(loader.hasExpiredEntries()).isFalse();
	}

	@Test
	void shouldDetectExpiredWhenMixedFreshAndExpiredEntries() throws Exception
	{
		Map<EntityID, CachedTrustChain> cache = new ConcurrentHashMap<>();
		cache.put(LEAF_ID, new CachedTrustChain(buildChain(), Instant.now().plusSeconds(3600)));
		cache.put(new EntityID("https://other.example.com"),
				new CachedTrustChain(buildChain(), Instant.now().minusSeconds(1)));
		OAuthFederationLoader loader = new OAuthFederationLoader(cache);

		assertThat(loader.hasExpiredEntries()).isTrue();
	}

	@Test
	void shouldClearAllEntriesOnInvalidate() throws Exception
	{
		Map<EntityID, CachedTrustChain> cache = cacheWith(LEAF_ID, buildChain(), Instant.now().minusSeconds(1));
		OAuthFederationLoader loader = new OAuthFederationLoader(cache);

		loader.invalidateCache();

		assertThat(loader.hasExpiredEntries()).isFalse();
		assertThat(cache).isEmpty();
	}

	// --- helpers ---

	private Map<EntityID, CachedTrustChain> cacheWith(EntityID id, TrustChain chain, Instant expiresAt)
	{
		Map<EntityID, CachedTrustChain> cache = new ConcurrentHashMap<>();
		cache.put(id, new CachedTrustChain(chain, expiresAt));
		return cache;
	}

	private TrustChain buildChain() throws Exception
	{
		ECKey key = new ECKeyGenerator(Curve.P_256).keyID("test").generate();
		JWKSet jwks = new JWKSet(key.toPublicJWK());
		Date now = new Date();
		Date exp = Date.from(Instant.now().plusSeconds(3600));

		EntityStatementClaimsSet leafClaims = new EntityStatementClaimsSet(
				LEAF_ID, LEAF_ID, now, exp, jwks);
		EntityStatement leafStatement = EntityStatement.sign(leafClaims, key);

		EntityStatementClaimsSet anchorClaims = new EntityStatementClaimsSet(
				TRUST_ANCHOR_ID, LEAF_ID, now, exp, jwks);
		EntityStatement anchorStatement = EntityStatement.sign(anchorClaims, key);

		return new TrustChain(leafStatement, List.of(anchorStatement));
	}
}
