/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import java.time.Duration;
import java.time.Instant;

import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.client.AttributeFetchResult;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;

/**
 * Cache of token validation results. Keys are access tokens. Values are validation results and
 * (if positive) also the obtained attributes.
 *  
 * @author K. Benedyczak
 */
public class ResultsCache
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ResultsCache.class);
	private static final Duration MAX_CACHE = Duration.ofHours(1); 
	private Cache<String, CacheEntry> resultsCache;
	private boolean perEntryTtl;
	private int globalTtl;
	private boolean disable = false;
	
	/**
	 * 
	 * @param cacheManager
	 * @param ttl positive value sets a global TTL. Negative orders to use per entry TTL. 
	 * Zero value disables caching.
	 */
	public ResultsCache(int ttl)
	{
		resultsCache = CacheBuilder.newBuilder().expireAfterWrite(MAX_CACHE).build();
		if (ttl < 0)
		{
			perEntryTtl = true;
		} else if (ttl > 0)
		{
			perEntryTtl = false;
			globalTtl = ttl;
		} else
		{
			disable = true;
		}
	}
		
	public CacheEntry getCached(String id)
	{
		if (disable)
			return null;
		CacheEntry entry = resultsCache.getIfPresent(id);
		if (entry == null || entry.isExpired())
			return null;
		log.debug("Using cached token validation result for " + entry.getTokenStatus().getSubject() + ": " + 
				entry.getTokenStatus().isValid() + " " + entry.getExpirationTime());
		return entry;
	}
	
	public void cache(String id, TokenStatus status, AttributeFetchResult attrs)
	{
		if (disable)
			return;
		CacheEntry entry = new CacheEntry(status, attrs);
		log.debug("Caching token validation result for {} status: {} token expiries {} cache expires {}",
				status.getSubject(), status.isValid(), status.getExpirationTime(), 
				entry.getExpirationTime());
		resultsCache.put(id, entry);
	}
	
	
	class CacheEntry
	{
		private TokenStatus tokenStatus;
		private AttributeFetchResult attributes;
		private Instant createTS;
		
		public CacheEntry(TokenStatus tokenStatus, AttributeFetchResult attributes)
		{
			this.tokenStatus = tokenStatus;
			this.attributes = attributes;
			this.createTS = Instant.now();
		}

		TokenStatus getTokenStatus()
		{
			return tokenStatus;
		}

		void setTokenStatus(TokenStatus tokenStatus)
		{
			this.tokenStatus = tokenStatus;
		}

		AttributeFetchResult getAttributes()
		{
			return attributes;
		}

		void setAttributes(AttributeFetchResult attributes)
		{
			this.attributes = attributes;
		}
		
		private Instant getExpirationTime()
		{
			Instant tokenExpiration = getTokenExpiration();
			if (perEntryTtl)
			{
				if (tokenExpiration != null)
				{
					return tokenExpiration;
				} else
				{
					return createTS.plusSeconds(OAuthRPProperties.DEFAULT_CACHE_TTL);
				}
			} else
			{
				Instant globalExpiry = createTS.plusSeconds(globalTtl);
				if (tokenExpiration == null)
					return globalExpiry;
				return tokenExpiration.isBefore(globalExpiry) ? tokenExpiration : globalExpiry;
			}
		}
		
		private boolean isExpired()
		{
			return getExpirationTime().isBefore(Instant.now()); 
		}
		
		private Instant getTokenExpiration()
		{
			if (tokenStatus.getExpirationTime() != null)
			{
				return Instant.ofEpochMilli(tokenStatus.getExpirationTime().getTime());
			} else
			{
				return null;
			}
		}
	}
}
