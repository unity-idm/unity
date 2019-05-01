/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.exceptions;

public class IllegalURIException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public IllegalURIException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalURIException(String msg)
	{
		super(msg);
	}
}
