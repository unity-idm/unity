/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Provides server-wide thread pool.
 * 
 * @author K. Benedyczak
 */
@Component
public class ExecutorsService
{
	private ScheduledExecutorService executors;
	
	@Autowired
	public ExecutorsService(UnityServerConfiguration cfg)
	{
		executors = Executors.newScheduledThreadPool(cfg.getIntValue(UnityServerConfiguration.THREAD_POOL_SIZE));
	}
	
	public ScheduledExecutorService getService()
	{
		return executors;
	}
}
