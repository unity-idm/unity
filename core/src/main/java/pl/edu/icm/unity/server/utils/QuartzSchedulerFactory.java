/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;
import org.quartz.spi.JobStore;
import org.quartz.spi.ThreadPool;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Creates a system wide {@link Scheduler}
 * @author K. Benedyczak
 */
@Configuration
public class QuartzSchedulerFactory
{
	public static final String NAME = "UNITY actions scheduler";

	@Bean
	public Scheduler getInstance() throws SchedulerException
	{
		JobStore jobStore = new RAMJobStore();
		DirectSchedulerFactory f = DirectSchedulerFactory.getInstance();
		ThreadPool threadPool = new SimpleThreadPool(3, Thread.NORM_PRIORITY-1);
		f.createScheduler(NAME, "default", threadPool, jobStore);
		SchedulerFactory schedFact = new StdSchedulerFactory();
		Scheduler sched = schedFact.getScheduler(NAME);
		sched.start();
		return sched;
	}
}
