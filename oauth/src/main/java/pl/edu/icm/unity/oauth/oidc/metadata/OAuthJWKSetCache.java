/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.oidc.metadata;

import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nimbusds.jose.jwk.JWKSet;

import pl.edu.icm.unity.base.utils.Log;

@Component
public class OAuthJWKSetCache
{
	private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(3);
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuthJWKSetCache.class);

	private final Cache<MetaCacheKey, JWKSet> cache;
	private final KeyResource downloader;

	OAuthJWKSetCache()
	{
		this(new KeyResource(), DEFAULT_CACHE_TTL);
	}

	OAuthJWKSetCache(KeyResource downloader, Duration ttl)
	{
		this.downloader = downloader;
		this.cache = CacheBuilder.newBuilder()
				.expireAfterWrite(ttl)
				.build();
	}

	public synchronized JWKSet getMetadata(JWKSetRequest jwkSetRequest) throws ParseException, IOException
	{
		MetaCacheKey metaCacheKey = new MetaCacheKey(jwkSetRequest.url, jwkSetRequest.validatorName,
				jwkSetRequest.hostnameChecking.name());
		JWKSet element = cache.getIfPresent(metaCacheKey);
		if (element != null)
		{
			log.debug("Get JWKSet from cache " + jwkSetRequest.url);
			return element;
		} else
		{
			log.debug("Get fresh JWKSet from " + jwkSetRequest.url);
			JWKSet metadata = downloader.getJWKSet(jwkSetRequest);
			cache.put(metaCacheKey, metadata);
			return metadata;
		}
	}

	public void clear()
	{
		cache.invalidateAll();
	}

}
