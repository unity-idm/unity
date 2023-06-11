/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.engine.api.files;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * 
 * @author P.Piernik
 *
 */
public class IllegalURIException extends EngineException
{
	public IllegalURIException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalURIException(String msg)
	{
		super(msg);
	}
}
