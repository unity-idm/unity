/**********************************************************************
 *                     Copyright (c) 2019, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package io.imunity.perfromance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceTestExecutor
{
	private static final Logger LOG = LoggerFactory.getLogger(PerformanceTestExecutor.class);
	private final PerformanceTestProvider singleOperationUnderTestProvider;
	private final int numberOfThreads;
	
	public PerformanceTestExecutor(PerformanceTestProvider singleOperationUnderTestProvider, int numberOfThreads)
	{
		this.singleOperationUnderTestProvider = singleOperationUnderTestProvider;
		this.numberOfThreads = numberOfThreads;
	}

	public void run(TimeUnit timeUnit, long timeout) throws InterruptedException
	{
		List<LoopedThread> loopedThreads = start();
		
		LOG.debug("Threads started, sleeping");
		timeUnit.sleep(timeout);
		
		LOG.debug("stopping threads");
		stop(loopedThreads);
		
		LOG.debug("test finished");
	}
	
	private List<LoopedThread> start()
	{
		List<LoopedThread> loopedThreads = new ArrayList<>(numberOfThreads);
		for (int i = 0; i < numberOfThreads; i++)
			loopedThreads.add(new LoopedThread(singleOperationUnderTestProvider.get(i+1)));
		loopedThreads.forEach(LoopedThread::start);
		return loopedThreads;
	}
	
	private void stop(List<LoopedThread> loopedThreads) throws InterruptedException
	{
		for (LoopedThread thread : loopedThreads)
			thread.stop();
	}
	
	static class LoopedThread
	{
		private final Thread thread;
		private final PerformanceTestRunnable singleOperationUnderTest;
		private final LoopedRunnable loopedRunnable;
		
		LoopedThread(PerformanceTestRunnable singleOperationUnderTest)
		{
			this.singleOperationUnderTest = singleOperationUnderTest;
			this.loopedRunnable = new LoopedRunnable(singleOperationUnderTest);
			this.thread = new Thread(loopedRunnable);
			this.thread.setDaemon(true);
		}
		
		void start()
		{
			singleOperationUnderTest.beforeRun();
			thread.start();
		}
		
		void stop() throws InterruptedException
		{
			loopedRunnable.stop();
			thread.join();
			singleOperationUnderTest.afterRun();
		}
	}
	
	static class LoopedRunnable implements Runnable
	{
		private final Runnable runnable;
		private boolean working = true;
		
		LoopedRunnable(Runnable runnable)
		{
			this.runnable = runnable;
		}

		@Override
		public void run()
		{
			while (this.working)
			{
				runnable.run();
			}
		}
		
		public void stop()
		{
			this.working = false;
		}
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private PerformanceTestProvider singleOperationUnderTestProvider;
		private int numberOfThreads;

		private Builder()
		{
		}

		public Builder withSingleOperationProvider(PerformanceTestProvider singleOperationUnderTestProvider)
		{
			this.singleOperationUnderTestProvider = singleOperationUnderTestProvider;
			return this;
		}

		public Builder withNumberOfThreads(int numberOfThreads)
		{
			this.numberOfThreads = numberOfThreads;
			return this;
		}

		public PerformanceTestExecutor build()
		{
			return new PerformanceTestExecutor(singleOperationUnderTestProvider, numberOfThreads);
		}
	}
}
