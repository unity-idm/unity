/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
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
import com.nimbusds.openid.connect.sdk.federation.entities.EntityStatement;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityType;
import com.nimbusds.openid.connect.sdk.federation.entities.FederationEntityMetadata;
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

	List<TrustChain> loadAll(OAuthFederationTrustConfig config) throws IOException, ParseException
	{
		List<EntityID> entityIds = fetchEntityListing(config);
		cache.keySet().retainAll(new HashSet<>(entityIds));
		if (entityIds.isEmpty())
			return List.of();
		TrustChainResolver resolver = buildResolver(config);
		List<TrustChain> result = new ArrayList<>();
		for (EntityID entityId : entityIds)
		{
			resolveWithCache(entityId, resolver, config).ifPresent(result::add);
		}
		if (result.isEmpty())
			throw new IOException("Failed to resolve any of " + entityIds.size()
					+ " listed federation entities");
		return result;
	}

	private List<EntityID> fetchEntityListing(OAuthFederationTrustConfig config) throws IOException, ParseException
	{
		URI listEndpoint = discoverListEndpoint(config);
		HTTPRequest httpRequest = new EntityListingRequest(listEndpoint, EntityType.OPENID_PROVIDER).toHTTPRequest();
		configurer.secureRequest(httpRequest, config.validator(), config.hostnameCheckingMode());
		HTTPResponse response = httpRequest.send();
		return EntityListingSuccessResponse.parse(response).getEntityListing();
	}

	private URI discoverListEndpoint(OAuthFederationTrustConfig config) throws IOException
	{
		TlsEntityStatementRetriever retriever = new TlsEntityStatementRetriever(
				config.validator(), config.hostnameCheckingMode());
		EntityStatement entityConfig;
		try
		{
			entityConfig = retriever.fetchEntityConfiguration(config.trustAnchorEntityId());
		} catch (ResolveException e)
		{
			throw new IOException("Failed to fetch entity configuration for trust anchor "
					+ config.trustAnchorEntityId(), e);
		}
		FederationEntityMetadata fedMeta = entityConfig.getClaimsSet().getFederationEntityMetadata();
		if (fedMeta == null || fedMeta.getFederationListEndpointURI() == null)
			throw new IOException("Trust anchor " + config.trustAnchorEntityId()
					+ " does not advertise federation_list_endpoint in its entity configuration");
		return fedMeta.getFederationListEndpointURI();
	}

	private Optional<TrustChain> resolveWithCache(EntityID entityId, TrustChainResolver resolver,
			OAuthFederationTrustConfig config)
	{
		CachedTrustChain cached = cache.get(entityId);
		if (cached != null && !cached.isExpired())
			return Optional.of(cached.trustChain());
		return resolveFresh(entityId, resolver, config);
	}

	private Optional<TrustChain> resolveFresh(EntityID entityId, TrustChainResolver resolver,
			OAuthFederationTrustConfig config)
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

	private TrustChainResolver buildResolver(OAuthFederationTrustConfig config)
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
