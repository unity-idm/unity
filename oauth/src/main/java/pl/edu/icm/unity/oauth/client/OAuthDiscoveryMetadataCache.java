/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;

@Component
class OAuthDiscoveryMetadataCache
{
	private static final Duration DEFAULT_CACHE_TTL = Duration.ofHours(3);
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2Verificator.class);

	private static final String CACHE_ID = OAuthDiscoveryMetadataCache.class.getName() + "_cache";

	private Ehcache cache;
	private OpenIdConnectDiscovery downloader;

	@Autowired
	OAuthDiscoveryMetadataCache(CacheProvider cacheProvider)
	{
		this(cacheProvider, new OpenIdConnectDiscovery(), DEFAULT_CACHE_TTL);
	}

	OAuthDiscoveryMetadataCache(CacheProvider cacheProvider, OpenIdConnectDiscovery downloader, Duration ttl)
	{
		this.downloader = downloader;
		initCache(cacheProvider.getManager(), ttl);
	}

	synchronized OIDCProviderMetadata getMetadata(CustomProviderProperties properties)
			throws ParseException, IOException
	{
		String url = properties.getValue(CustomProviderProperties.OPENID_DISCOVERY);
		MetaCacheKey metaCacheKey = new MetaCacheKey(url,
				properties.getValue(CustomProviderProperties.CLIENT_TRUSTSTORE),
				properties.getValue(CustomProviderProperties.CLIENT_HOSTNAME_CHECKING));
		cache.evictExpiredElements();
		Element element = cache.get(metaCacheKey);
		if (element != null)
		{
			log.trace("Get oauth OIDC metadata provider from cache " + url);
			return ((OIDCProviderMetadata) element.getObjectValue());
		} else
		{
			log.trace("Get fresh oauth OIDC metadata from " + url);
			OIDCProviderMetadata metadata = downloader.getMetadata(url, properties);
			cache.put(new Element(metaCacheKey, metadata));
			return metadata;
		}
	}

	private void initCache(CacheManager cacheManager, Duration cacheTTL)
	{
		cache = cacheManager.addCacheIfAbsent(CACHE_ID);
		CacheConfiguration config = cache.getCacheConfiguration();
		config.setTimeToIdleSeconds(cacheTTL.toSeconds());
		config.setTimeToLiveSeconds(cacheTTL.toSeconds());
		PersistenceConfiguration persistCfg = new PersistenceConfiguration();
		persistCfg.setStrategy("none");
		config.persistence(persistCfg);
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
