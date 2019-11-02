/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.exceptions;

/**
 * Thrown when capacity limit is reached.
 * 
 * @author P.Piernik
 *
 */
public class CapacityLimitReachedException extends EngineException
{

	public CapacityLimitReachedException(String msg)
	{
		super(msg);
	}
}
