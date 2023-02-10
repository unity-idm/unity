/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.oidc.metadata;

import java.io.IOException;
import java.time.Duration;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.base.utils.Log;

@Component
public class OAuthDiscoveryMetadataCache
{
	private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(3);
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthDiscoveryMetadataCache.class);

	private final Cache<MetaCacheKey, OIDCProviderMetadata> cache;
	private final OpenIdConnectDiscovery downloader;

	OAuthDiscoveryMetadataCache()
	{
		this(new OpenIdConnectDiscovery(), DEFAULT_CACHE_TTL);
	}

	OAuthDiscoveryMetadataCache(OpenIdConnectDiscovery downloader, Duration ttl)
	{
		this.downloader = downloader;
		this.cache = CacheBuilder.newBuilder()
				.expireAfterWrite(ttl)
				.build();
	}

	public synchronized OIDCProviderMetadata getMetadata(OIDCMetadataRequest oidcMetadataRequest)
			throws ParseException, IOException
	{
		MetaCacheKey metaCacheKey = new MetaCacheKey(oidcMetadataRequest.url,
				oidcMetadataRequest.validatorName,
				oidcMetadataRequest.hostnameChecking.name());
		OIDCProviderMetadata element = cache.getIfPresent(metaCacheKey);
		if (element != null)
		{
			log.debug("Get oauth OIDC metadata provider from cache " + oidcMetadataRequest.url);
			return element;
		} else
		{
			log.debug("Get fresh oauth OIDC metadata from " + oidcMetadataRequest.url);
			OIDCProviderMetadata metadata = downloader.getMetadata(oidcMetadataRequest);
			cache.put(metaCacheKey, metadata);
			return metadata;
		}
	}

	public void clear()
	{
		cache.invalidateAll();
	}
}
