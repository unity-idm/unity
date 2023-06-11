/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.capacityLimits;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import pl.edu.icm.unity.base.capacityLimit.CapacityLimit;
import pl.edu.icm.unity.base.capacityLimit.CapacityLimitName;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.exceptions.CapacityLimitReachedException;
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
	private static final Logger log = Log.getLogger(Log.U_SERVER_CORE, InternalCapacityLimitVerificator.class);

	private LoadingCache<String, Map<String, Integer>> limitCache;

	@Autowired
	public InternalCapacityLimitVerificator(CapacityLimitDB limitDB)
	{
		limitCache = initCache(limitDB);
	}

	private LoadingCache<String, Map<String, Integer>> initCache(CapacityLimitDB limitDB)
	{
		return CacheBuilder.newBuilder().expireAfterAccess(120, TimeUnit.SECONDS)
				.build(new CacheLoader<String, Map<String, Integer>>()
				{
					public Map<String, Integer> load(String name)
					{
						log.trace("Get fresh values of capacity limits");
						return limitDB.getAll().stream().collect(Collectors.toMap(
								CapacityLimit::getName, CapacityLimit::getValue));

					}
				});
	}

	public void assertInSystemLimitForSingleAdd(CapacityLimitName name, Supplier<Long> value)
			throws CapacityLimitReachedException
	{
		assertInSystemLimit(name, () -> value.get() + 1);
	}

	public void assertInSystemLimit(CapacityLimitName name, Supplier<Long> value) throws CapacityLimitReachedException
	{
		Map<String, Integer> limits = limitCache.getUnchecked("");
		int limit = -1;
		if (limits.containsKey(name.toString()))
		{
			limit = limits.get(name.toString());
		}

		log.trace("Checks capacity limit for {} limit={}  value={}", name.toString(), limit, value);
		if (limit < 0)
			return;

		if (limit < value.get())
		{
			log.info("Capacity limit {} reached (limit={}, value={})", name.toString(), limit, value);
			throw new CapacityLimitReachedException("Capacity limit reached");
		}

	}

	public void clearCache()
	{
		limitCache.invalidateAll();
	}
}
