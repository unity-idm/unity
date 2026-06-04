/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.federation.api.EntityListingRequest;
import com.nimbusds.openid.connect.sdk.federation.api.EntityListingSuccessResponse;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.trust.ResolveException;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChain;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainResolver;
import com.nimbusds.openid.connect.sdk.federation.trust.TrustChainSet;
import com.nimbusds.openid.connect.sdk.federation.trust.constraints.TrustChainConstraints;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;

class OAuthFederationLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthFederationLoader.class);

	private final Map<EntityID, CachedTrustChain> cache;
	private final HttpRequestConfigurer configurer = new HttpRequestConfigurer();

	OAuthFederationLoader()
	{
		this.cache = new ConcurrentHashMap<>();
	}

	OAuthFederationLoader(Map<EntityID, CachedTrustChain> cache)
	{
		this.cache = cache;
	}

	List<TrustChain> loadAll(OAuthFederationConfig config)
	{
		List<EntityID> entityIds = fetchEntityListing(config);
		TrustChainResolver resolver = buildResolver(config);
		List<TrustChain> result = new ArrayList<>();
		for (EntityID entityId : entityIds)
		{
			resolveWithCache(entityId, resolver, config).ifPresent(result::add);
		}
		return result;
	}

	private List<EntityID> fetchEntityListing(OAuthFederationConfig config)
	{
		try
		{
			HTTPRequest httpRequest = new EntityListingRequest(
					config.trustAnchorListEndpoint(), EntityType.OPENID_PROVIDER).toHTTPRequest();
			configurer.secureRequest(httpRequest, config.validator(), config.hostnameCheckingMode());
			HTTPResponse response = httpRequest.send();
			return EntityListingSuccessResponse.parse(response).getEntityListing();
		} catch (ParseException | IOException e)
		{
			log.error("Failed to fetch entity listing from {}", config.trustAnchorListEndpoint(), e);
			return List.of();
		}
	}

	private Optional<TrustChain> resolveWithCache(EntityID entityId, TrustChainResolver resolver,
			OAuthFederationConfig config)
	{
		CachedTrustChain cached = cache.get(entityId);
		if (cached != null && !cached.isExpired())
			return Optional.of(cached.trustChain());
		return resolveFresh(entityId, resolver, config);
	}

	private Optional<TrustChain> resolveFresh(EntityID entityId, TrustChainResolver resolver,
			OAuthFederationConfig config)
	{
		try
		{
			TrustChainSet chains = resolver.resolveTrustChains(entityId);
			TrustChain shortest = chains.getShortest();
			if (shortest == null)
				return Optional.empty();
			Instant expiresAt = shortest.resolveExpirationTime().toInstant();
			cache.put(entityId, new CachedTrustChain(shortest, expiresAt));
			return Optional.of(shortest);
		} catch (ResolveException e)
		{
			log.warn("Failed to resolve trust chain for entity {}", entityId, e);
			return Optional.empty();
		}
	}

	private TrustChainResolver buildResolver(OAuthFederationConfig config)
	{
		TlsEntityStatementRetriever retriever = new TlsEntityStatementRetriever(
				config.validator(), config.hostnameCheckingMode());
		return new TrustChainResolver(
				Map.of(config.trustAnchorEntityId(), config.trustAnchorJwks()),
				TrustChainConstraints.NO_CONSTRAINTS,
				retriever);
	}

	boolean hasExpiredEntries()
	{
		return cache.values().stream().anyMatch(CachedTrustChain::isExpired);
	}

	void invalidateCache()
	{
		cache.clear();
	}

	record CachedTrustChain(TrustChain trustChain, Instant expiresAt)
	{
		boolean isExpired()
		{
			return Instant.now().isAfter(expiresAt);
		}
	}
}
