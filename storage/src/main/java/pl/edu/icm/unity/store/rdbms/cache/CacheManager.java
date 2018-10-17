/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.store.StorageConfiguration;
import pl.edu.icm.unity.store.StorageEngine;
import pl.edu.icm.unity.store.rdbms.RDBMSConfiguration;

/**
 * Allows for coordination of all caches, like global flushing
 * 
 * @author K. Benedyczak
 */
@Component
public class CacheManager
{
	private Collection<BasicCache<?>> platformCaches = new ArrayList<>();
	
	private RDBMSConfiguration storageConfig;

	@Autowired
	public CacheManager(StorageConfiguration storageConfig)
	{
		if (storageConfig.getEngine() == StorageEngine.rdbms)
			this.storageConfig = storageConfig.getEngineConfig();
	}

	public void registerCache(BasicCache<?> cache)
	{
		platformCaches.add(cache);
		if (storageConfig != null)
			cache.configure(storageConfig.getIntValue(RDBMSConfiguration.CACHE_TTL), 
					storageConfig.getIntValue(RDBMSConfiguration.CACHE_MAX_ENTRIES));
	}

	public void registerCacheWithFlushingPropagation(BasicCache<?> cache)
	{
		registerCache(cache);
		cache.setFlushListener(this::flushAllCaches);
	}
	
	public void flushAllCaches()
	{
		for (BasicCache<?> cache: platformCaches)
			cache.flushWithoutEvent();
	}
}
