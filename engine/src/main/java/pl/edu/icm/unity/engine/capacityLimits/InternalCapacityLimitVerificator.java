/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.capacityLimits;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import pl.edu.icm.unity.base.capacityLimit.CapacityLimitName;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.CapacityLimitReachedException;
import pl.edu.icm.unity.store.api.generic.CapacityLimitDB;

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

	private LoadingCache<String, Integer> limitCache;

	@Autowired
	public InternalCapacityLimitVerificator(CapacityLimitDB limitDB)
	{
		limitCache = CacheBuilder.newBuilder().expireAfterAccess(120, TimeUnit.SECONDS)
				.build(new CacheLoader<String, Integer>()
				{
					public Integer load(String name)
					{
						if (limitDB.exists(name))
						{
							int limit = limitDB.get(name).getValue();
							log.trace("Get fresh value of capacity limit {} for {}", limit,
									name);
							return limit;
						} else
						{
							log.trace("Empty capacity limit for {}", name);
							return -1;
						}
					}
				});
	}

	public void assertInSystemLimitForSingleAdd(CapacityLimitName name, long value)
			throws CapacityLimitReachedException
	{
		assertInSystemLimit(name, value + 1);
	}

	public void assertInSystemLimit(CapacityLimitName name, long value) throws CapacityLimitReachedException
	{
		int limit = limitCache.getUnchecked(name.toString());
		if (limit < 0)
			return;

		log.trace("Checks capacity limit for {} limit={}  value={}", name.toString(), limit, value);

		if (limit < value)
		{
			log.debug("Capacity limit {} reached (limit={}, value={})", name.toString(), limit, value);
			throw new CapacityLimitReachedException("Capacity limit reached");
		}

	}

	public void clearCache()
	{
		limitCache.invalidateAll();
	}
}
