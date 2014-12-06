/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import java.util.Date;
import java.util.Map;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;
import pl.edu.icm.unity.server.utils.Log;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;

/**
 * Cache of token validation results. Keys are access tokens. Values are validation results and
 * (if positive) also the obtained attributes.
 *  
 * @author K. Benedyczak
 */
public class ResultsCache
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ResultsCache.class);
	private static final String CACHE_ID = ResultsCache.class.getName();
	private Ehcache resultsCache;
	private boolean perEntryTtl;
	private int globalTtl;
	private boolean disable = false;
	
	/**
	 * 
	 * @param cacheManager
	 * @param ttl positive value sets a global TTL. Negative orders to use per entry TTL. 
	 * Zero value disables caching.
	 */
	public ResultsCache(CacheManager cacheManager, int ttl)
	{
		resultsCache = cacheManager.addCacheIfAbsent(CACHE_ID);
		CacheConfiguration config = resultsCache.getCacheConfiguration();
		if (ttl < 0)
		{
			config.setTimeToIdleSeconds(180);
			config.setTimeToLiveSeconds(180);
			perEntryTtl = true;
		} else if (ttl > 0)
		{
			config.setTimeToIdleSeconds(ttl);
			config.setTimeToLiveSeconds(ttl);
			perEntryTtl = false;
			globalTtl = ttl;
		} else
		{
			disable = true;
		}
		PersistenceConfiguration persistCfg = new PersistenceConfiguration();
		persistCfg.setStrategy("none");
		config.persistence(persistCfg);
	}
		
	public CacheEntry getCached(String id)
	{
		if (disable)
			return null;
		Element entry = resultsCache.get(id);
		if (entry == null || entry.isExpired())
			return null;
		CacheEntry ret = (CacheEntry) entry.getObjectValue();
		log.debug("Using cached token validation result for " + ret.getTokenStatus().getSubject() + ": " + 
				ret.getTokenStatus().isValid() + " " + new Date(entry.getExpirationTime()));
		return ret;
	}
	
	public void cache(String id, TokenStatus status, Map<String, String> attributes)
	{
		if (disable)
			return;
		CacheEntry entry = new CacheEntry(status, attributes);
		int ttl = getCacheTtl(status);
		Element element = new Element(id, entry, ttl, ttl);
		log.debug("Caching token validation result for " + status.getSubject() + ": " + status.isValid() + 
				" expiry: " + new Date(element.getExpirationTime()));
		resultsCache.put(element);
	}
	
	private int getCacheTtl(TokenStatus status)
	{
		if (perEntryTtl)
		{
			if (status.getExpirationTime() != null)
			{
				long diff = status.getExpirationTime().getTime() - System.currentTimeMillis();
				int ttl = (int) (diff/1000);
				if (ttl > 3600)
					ttl = 3600;
				return ttl;
			} else
			{
				return OAuthRPProperties.DEFAULT_CACHE_TTL;
			}
		} else
		{
			return globalTtl;
		}
	}
	
	public static class CacheEntry
	{
		private TokenStatus tokenStatus;
		private Map<String, String> attributes;
		
		public CacheEntry(TokenStatus tokenStatus, Map<String, String> attributes)
		{
			super();
			this.tokenStatus = tokenStatus;
			this.attributes = attributes;
		}

		public TokenStatus getTokenStatus()
		{
			return tokenStatus;
		}

		public void setTokenStatus(TokenStatus tokenStatus)
		{
			this.tokenStatus = tokenStatus;
		}

		public Map<String, String> getAttributes()
		{
			return attributes;
		}

		public void setAttributes(Map<String, String> attributes)
		{
			this.attributes = attributes;
		}
	}
}
