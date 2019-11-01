/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

import org.bouncycastle.crypto.generators.SCrypt;

/**
 * Uses scrypt to encode passwords. The class limits number of concurrent encodings so that we can have a control
 * about the total memory use by scrypt.  
 * Scrypt memory appetite is: 128 bytes × 2^workFactor × blockSize.
 * With default block size of 8, it is 1024 x 2^workfactor.
 */
public class SCryptEncoder
{
	private ForkJoinPool pool;
	private long maxMemory;
	
	public SCryptEncoder(ForkJoinPool pool)
	{
		this(pool, Runtime.getRuntime().maxMemory());
	}

	SCryptEncoder(ForkJoinPool pool, long maxHeap)
	{
		this.pool = pool;
		this.maxMemory = maxHeap;
		
	}

	/**
	 * Computes a max currently considered safe work factor, taking into account the heap memory available for the VM
	 * and the configured number of allowed concurrent password checks. Algorithm assumes 0.5G heap reserved for 
	 * other purposes then password hashing (pretty arbitrary number, typically well enough, but may happen this is 
	 * to low). 
	 * 
	 * @return max WF so that 1024*2^WF < MAX_AVAILABLE_HEAP - 500MB   
	 */
	public int getMaxAllowedWorkFactor()
	{
		int parallelizm = pool.getParallelism();
		if (maxMemory == Long.MAX_VALUE)
			maxMemory = 1 << 30;
		
		long reservedMemory = 500_000_000;
		long availableMemory = maxMemory - reservedMemory;
		long availablePerThread = availableMemory / parallelizm;
		long maxCost = availablePerThread / 1024;
		int maxWorkFactor = -1;
		for (; maxCost != 0; maxWorkFactor++)
			maxCost = maxCost >> 1;
		if (maxWorkFactor < ScryptParams.MIN_WORK_FACTOR)
			return ScryptParams.MIN_WORK_FACTOR;
		if (maxWorkFactor > ScryptParams.MAX_WORK_FACTOR)
			return ScryptParams.MAX_WORK_FACTOR;
		return maxWorkFactor;
	}
	
	public byte[] scrypt(String password, byte[] salt, ScryptParams params)
	{
		ForkJoinTask<byte[]> task = pool.submit(() -> runScrypt(password, salt, params));
		try
		{
			return task.get();
		} catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted when scrypting", e);
		} catch (ExecutionException e)
		{
			throw new IllegalStateException("Error when scrypting", e);
		}
	}
	
	
	private byte[] runScrypt(String password, byte[] salt, ScryptParams params)
	{
		return SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), 
				salt,
				1 << params.getWorkFactor(), 
				params.getBlockSize(),
				params.getParallelization(),
				params.getLength());
	}
}
