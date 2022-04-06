/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.perfromance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class PerformanceTestExecutor
{
	private static final Logger LOG = LoggerFactory.getLogger(PerformanceTestExecutor.class);
	private final PerformanceTestProvider singleOperationUnderTestProvider;
	private final int numberOfThreads;
	private final PerformanceTestConfig config;
	
	public PerformanceTestExecutor(PerformanceTestProvider singleOperationUnderTestProvider,
			int numberOfThreads,
			PerformanceTestConfig config)
	{
		this.singleOperationUnderTestProvider = singleOperationUnderTestProvider;
		this.numberOfThreads = numberOfThreads;
		this.config = config;
	}

	public void run(TimeUnit timeUnit, long timeout) throws InterruptedException
	{
		List<LoopedThread> loopedThreads = start();
		
		LOG.debug("Threads started, sleeping");
		timeUnit.sleep(timeout);
		
		LOG.debug("stopping threads");
		List<LoopedRunnableStats> stats = stop(loopedThreads);
		
		printStats(stats, timeUnit, timeout);
		
		LOG.debug("test finished");
	}
	
	private void printStats(List<LoopedRunnableStats> stats, TimeUnit timeUnit, long timeout)
	{
		int iterations = stats.stream()
				.map(LoopedRunnableStats::getIterations)
				.reduce(0, Integer::sum);
		int successes = stats.stream()
				.map(LoopedRunnableStats::getSuccesses)
				.reduce(0, Integer::sum);
		List<Failure> failures = stats.stream()
				.map(LoopedRunnableStats::getFailures)
				.flatMap(List::stream).collect(Collectors.toList());
		
		double iterationsPerSecond = (double) iterations / (double) timeUnit.toSeconds(timeout);
		String failureDetails = failures.stream().map(Failure::toString).collect(Collectors.joining("  \n"));
		if (failures.isEmpty())
			failureDetails = "[]";
		LOG.info("\nTest duration: {} {}\nNumber of threads: {}\nAll iterations: {}\nOperations per second: {}\n"
						+ "Successes: {}\nFailures: {}\nFailure Details: {}",
				timeout, timeUnit.toString(), numberOfThreads, iterations, iterationsPerSecond, successes,
				failures.size(), failureDetails);
	}

	private List<LoopedThread> start()
	{
		List<LoopedThread> loopedThreads = new ArrayList<>(numberOfThreads);
		for (int i = 0; i < numberOfThreads; i++)
			loopedThreads.add(new LoopedThread(singleOperationUnderTestProvider.get(i+1, config)));
		loopedThreads.forEach(LoopedThread::start);
		return loopedThreads;
	}
	
	private List<LoopedRunnableStats> stop(List<LoopedThread> loopedThreads) throws InterruptedException
	{
		for (LoopedThread thread : loopedThreads)
			thread.stop();
		
		return loopedThreads.stream().map(LoopedThread::stats)
				.collect(Collectors.toList());
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
		
		LoopedRunnableStats stats()
		{
			return loopedRunnable.getStats();
		}
	}
	
	static class LoopedRunnable implements Runnable
	{
		private final PerformanceTestRunnable runnable;
		private final LoopedRunnableStats stats;
		private AtomicBoolean working = new AtomicBoolean(true);
		
		LoopedRunnable(PerformanceTestRunnable runnable)
		{
			this.runnable = runnable;
			this.stats = new LoopedRunnableStats();
		}

		@Override
		public void run()
		{
			while (this.working.get())
			{
				stats.incInteration();
				try
				{
					runnable.run();
					stats.incSuccess();
				} catch (Exception e)
				{
					String snapshot = runnable.takeScreenshot(stats.getIterations() + "");
					stats.addFailure(e, snapshot);
					runnable.reset();
				}
			}
		}

		void stop()
		{
			this.working.set(false);
		}

		LoopedRunnableStats getStats()
		{
			return stats;
		}
	}
	
	static class LoopedRunnableStats
	{
		private int iterations = 0;
		private int successes = 0;
		private List<Failure> failures = Lists.newArrayList();
		
		void incSuccess()
		{
			successes += 1;
		}
		
		void incInteration()
		{
			iterations += 1;
		}
		
		void addFailure(Exception e, String snapshot)
		{
			failures.add(new Failure(e, snapshot, LocalDate.now()));
		}

		int getSuccesses()
		{
			return successes;
		}

		int getIterations()
		{
			return iterations;
		}

		List<Failure> getFailures()
		{
			return failures;
		}
	}
	
	static class Failure
	{
		public final Exception e;
		public final String shapshot;
		public final LocalDate when;

		Failure(Exception e, String shapshot, LocalDate when)
		{
			this.e = e;
			this.shapshot = shapshot;
			this.when = when;
		}

		@Override
		public String toString()
		{
			return String.format("Cause: %s, at: %s, details: %s", e.getMessage(), when.toEpochDay(), shapshot);
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
		private PerformanceTestConfig config;

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

		public Builder withPerformanceTestConfig(PerformanceTestConfig config)
		{
			this.config = config;
			return this;
		}
		
		public PerformanceTestExecutor build()
		{
			return new PerformanceTestExecutor(singleOperationUnderTestProvider, numberOfThreads, config);
		}
	}
}
