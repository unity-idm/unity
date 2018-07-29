/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.cache;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;

/**
 * Allows for coordination of all caches, like global flushing
 * 
 * @author K. Benedyczak
 */
@Component
public class CacheManager
{
	private Collection<BasicCache<?>> platformCaches = new ArrayList<>();
	
	public void registerCache(BasicCache<?> cache)
	{
		platformCaches.add(cache);
	}
	
	public void flushAllCaches()
	{
		for (BasicCache<?> cache: platformCaches)
			cache.flush();
	}
}
