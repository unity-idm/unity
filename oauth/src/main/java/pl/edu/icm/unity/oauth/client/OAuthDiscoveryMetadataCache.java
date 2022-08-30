/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;

@Component
class OAuthDiscoveryMetadataCache
{
	private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(3);
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2Verificator.class);

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

	synchronized OIDCProviderMetadata getMetadata(CustomProviderProperties properties)
			throws ParseException, IOException
	{
		String url = properties.getValue(CustomProviderProperties.OPENID_DISCOVERY);
		MetaCacheKey metaCacheKey = new MetaCacheKey(url,
				properties.getValue(CustomProviderProperties.CLIENT_TRUSTSTORE),
				properties.getValue(CustomProviderProperties.CLIENT_HOSTNAME_CHECKING));
		OIDCProviderMetadata element = cache.getIfPresent(metaCacheKey);
		if (element != null)
		{
			log.debug("Get oauth OIDC metadata provider from cache " + url);
			return element;
		} else
		{
			log.debug("Get fresh oauth OIDC metadata from " + url);
			OIDCProviderMetadata metadata = downloader.getMetadata(url, properties);
			cache.put(metaCacheKey, metadata);
			return metadata;
		}
	}

	public void clear()
	{
		cache.invalidateAll();
	}

	private static class MetaCacheKey
	{
		final String url;
		final String validator;
		final String hostnameChecking;

		private MetaCacheKey(String url, String validator, String hostnameChecking)
		{
			this.url = url;
			this.validator = validator;
			this.hostnameChecking = hostnameChecking;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(hostnameChecking, url, validator);
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			MetaCacheKey other = (MetaCacheKey) obj;
			return Objects.equals(hostnameChecking, other.hostnameChecking) && Objects.equals(url, other.url)
					&& Objects.equals(validator, other.validator);
		}
	}
}
