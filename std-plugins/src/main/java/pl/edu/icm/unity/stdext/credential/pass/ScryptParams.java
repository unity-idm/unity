/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameters governing Scrypt algorithm.
 * 
 * Allows to be converted to Map, useful as copy of parameters needs to be stored with 
 * particular hashed password information.
 * @author K. Benedyczak
 */
public class ScryptParams
{
	public static final int MIN_WORK_FACTOR = 10;
	public static final int MAX_WORK_FACTOR = 23;
	
	private int workFactor;
	private int blockSize;
	private int parallelization;
	private int length;

	public ScryptParams(Map<String, Object> params)
	{
		this((Integer)params.get("workFactor"), (Integer)params.get("blockSize"), 
				(Integer)params.get("parallelization"), (Integer)params.get("length"));
	}
	
	public ScryptParams(int workFactor, int blockSize, int parallelization, int length)
	{
		this.workFactor = workFactor;
		this.blockSize = blockSize;
		this.parallelization = parallelization;
		this.length = length;
		sanitize();
	}
	
	public ScryptParams()
	{
		this(15);
	}
	
	public ScryptParams(int workFactor)
	{
		this(workFactor, 8, 1, 512/8);
	}
	
	public void sanitize()
	{
		if (workFactor < MIN_WORK_FACTOR)
			workFactor = MIN_WORK_FACTOR;
		if (workFactor > MAX_WORK_FACTOR)
			workFactor = MAX_WORK_FACTOR;
		if (blockSize < 8)
			blockSize = 8;
		if (blockSize > 1024*16)
			blockSize = 1024*16;
		if (parallelization < 1)
			parallelization = 1;
		if (parallelization > 64)
			parallelization = 64;
		if (length < 32)
			length = 32;
		if (length > 2048)
			length = 2048;
	}
	
	public int getWorkFactor()
	{
		return workFactor;
	}

	public int getBlockSize()
	{
		return blockSize;
	}

	public int getParallelization()
	{
		return parallelization;
	}

	public int getLength()
	{
		return length;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + blockSize;
		result = prime * result + length;
		result = prime * result + parallelization;
		result = prime * result + workFactor;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScryptParams other = (ScryptParams) obj;
		if (blockSize != other.blockSize)
			return false;
		if (length != other.length)
			return false;
		if (parallelization != other.parallelization)
			return false;
		if (workFactor != other.workFactor)
			return false;
		return true;
	}

	public Map<String, Object> toMap()
	{
		Map<String, Object> params = new HashMap<>();
		params.put("workFactor", workFactor);
		params.put("blockSize", blockSize);
		params.put("parallelization", parallelization);
		params.put("length", length);
		return params;
	}
}
