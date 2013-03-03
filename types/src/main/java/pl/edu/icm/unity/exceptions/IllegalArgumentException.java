/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

public class IllegalArgumentException extends RuntimeEngineException
{
	private static final long serialVersionUID = 1L;

	public IllegalArgumentException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalArgumentException(String msg)
	{
		super(msg);
	}
}
