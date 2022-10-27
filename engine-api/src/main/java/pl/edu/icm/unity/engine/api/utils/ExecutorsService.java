/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Provides server-wide thread pool.
 */
@Component
public class ExecutorsService
{
	private final ScheduledExecutorService scheduledExecutor;
	private final ExecutorService workStealingExecutor;
	
	@Autowired
	public ExecutorsService(UnityServerConfiguration cfg)
	{
		scheduledExecutor = Executors.newScheduledThreadPool(cfg.getIntValue(UnityServerConfiguration.SCHEDULED_THREAD_POOL_SIZE));
		workStealingExecutor = Executors.newWorkStealingPool(cfg.getIntValue(UnityServerConfiguration.CONCURRENT_THREAD_POOL_SIZE));
	}
	
	public ScheduledExecutorService getScheduledService()
	{
		return scheduledExecutor;
	}
	
	public ExecutorService getExecutionService()
	{
		return workStealingExecutor;
	}
}
