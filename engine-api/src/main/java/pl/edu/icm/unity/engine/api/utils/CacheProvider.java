/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.springframework.stereotype.Component;

@Component
public class CacheProvider
{
	private CacheManager cacheManager;
	
	
	public CacheProvider()
	{
		cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true); 
	}

	public CacheManager getManager()
	{
		return cacheManager;
	}
}
