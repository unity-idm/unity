/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.exceptions;

import pl.edu.icm.unity.base.exceptions.EngineException;

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
