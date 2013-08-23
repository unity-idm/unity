/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;

import org.springframework.stereotype.Component;

/**
 * Component providing access to Ehcache CacheManager. 
 * @author K. Benedyczak
 */
@Component
public class CacheProvider
{
	private CacheManager cacheManager;
	
	
	public CacheProvider()
	{
		Configuration config = new Configuration();
		config.setName("UNITY cache manager");
		config.setUpdateCheck(false);
		config.setMaxBytesLocalHeap(10240000L);
		
		CacheConfiguration defC = new CacheConfiguration();
		config.addDefaultCache(defC);
		cacheManager = CacheManager.create(config);
	}

	public CacheManager getManager()
	{
		return cacheManager;
	}
}
