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
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceTestExecutor
{
	private static final Logger LOG = LoggerFactory.getLogger(PerformanceTestExecutor.class);
	private final Supplier<PerformanceTestRunnable> singleOperationUnderTestSupplier;
	private final int numberOfThreads;
	
	public PerformanceTestExecutor(Supplier<PerformanceTestRunnable> singleOperationUnderTestSupplier, int numberOfThreads)
	{
		this.singleOperationUnderTestSupplier = singleOperationUnderTestSupplier;
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
			loopedThreads.add(new LoopedThread(singleOperationUnderTestSupplier.get()));
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
		
		LoopedThread(PerformanceTestRunnable singleOperationUnderTest)
		{
			this.singleOperationUnderTest = singleOperationUnderTest;
			this.thread = new Thread(new LoopedRunnable(singleOperationUnderTest));
		}
		
		void start()
		{
			singleOperationUnderTest.beforeRun();
			thread.start();
		}
		
		void stop() throws InterruptedException
		{
			thread.interrupt();
			thread.join();
			singleOperationUnderTest.afterRun();
		}
	}
	
	static class LoopedRunnable implements Runnable
	{
		private final Runnable runnable;
		
		LoopedRunnable(Runnable runnable)
		{
			this.runnable = runnable;
		}

		@Override
		public void run()
		{
			while (!Thread.currentThread().isInterrupted())
			{
				runnable.run();
			}
		}
	}

	public static Builder builder()
	{
		return new Builder();
	}

	public static final class Builder
	{
		private Supplier<PerformanceTestRunnable> singleOperationUnderTestSupplier;
		private int numberOfThreads;

		private Builder()
		{
		}

		public Builder withSingleOperationSupplier(Supplier<PerformanceTestRunnable> singleOperationUnderTestSupplier)
		{
			this.singleOperationUnderTestSupplier = singleOperationUnderTestSupplier;
			return this;
		}

		public Builder withNumberOfThreads(int numberOfThreads)
		{
			this.numberOfThreads = numberOfThreads;
			return this;
		}

		public PerformanceTestExecutor build()
		{
			return new PerformanceTestExecutor(singleOperationUnderTestSupplier, numberOfThreads);
		}
	}
}
