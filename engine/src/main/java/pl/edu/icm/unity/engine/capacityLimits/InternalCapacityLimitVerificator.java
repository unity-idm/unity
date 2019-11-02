/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.capacityLimits;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.utils.CacheProvider;
import pl.edu.icm.unity.exceptions.CapacityLimitReachedException;
import pl.edu.icm.unity.store.api.generic.CapacityLimitDB;
import pl.edu.icm.unity.types.capacityLimit.CapacityLimitName;

/**
 * Capacity limit verificator. Use caching to increase the speed of capacity
 * limit checks. For internal use only.
 * 
 * @author P.Piernik
 *
 */
@Component
public class InternalCapacityLimitVerificator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, InternalCapacityLimitVerificator.class);
	private static final String CACHE_ID = InternalCapacityLimitVerificator.class.getName() + "_cache";

	private CapacityLimitDB limitDB;
	private Ehcache limitCache;

	@Autowired
	public InternalCapacityLimitVerificator(CacheProvider cacheProvider, CapacityLimitDB limitDB)
	{
		this.limitDB = limitDB;
		initCache(cacheProvider.getManager());
	}

	private void initCache(CacheManager cacheManager)
	{
		limitCache = cacheManager.addCacheIfAbsent(CACHE_ID);
		CacheConfiguration config = limitCache.getCacheConfiguration();
		config.setTimeToIdleSeconds(120);
		config.setTimeToLiveSeconds(120);
		PersistenceConfiguration persistCfg = new PersistenceConfiguration();
		persistCfg.setStrategy("none");
		config.persistence(persistCfg);
	}

	public void assertInSystemLimitForSingleAdd(CapacityLimitName name, long value)
			throws CapacityLimitReachedException
	{
		assertInSystemLimit(name, value + 1);
	}

	public void assertInSystemLimit(CapacityLimitName name, long value) throws CapacityLimitReachedException
	{
		int limit = getLimit(name.toString());
		if (limit < 0)
			return;

		log.debug("Checks capacity limit for {} limit ={}  value={}", name.toString(), limit, value);

		if (limit < value)
		{
			log.debug("Capacity limit {} reached (limit={}, value={})", name.toString(), limit, value);
			throw new CapacityLimitReachedException("Capacity limit reached");
		}

	}

	private int getLimit(String name)
	{
		limitCache.evictExpiredElements();
		int limit;
		Element cached = limitCache.get(name);
		if (cached != null && !cached.isExpired())
		{
			limit = (int) cached.getObjectValue();
			log.debug("Returning cached capacity limit {} for {}", limit, name);

		} else
		{
			try
			{
				limit = limitDB.get(name).getValue();
				limitCache.put(new Element(name, limit));
			} catch (Exception e)
			{
				// ok, empty limit
				log.debug("Empty capacity limit for {}", name);
				return -1;
			}
		}
		return limit;
	}
	
	
	public void clearCache()
	{
		limitCache.removeAll();
	}
}
