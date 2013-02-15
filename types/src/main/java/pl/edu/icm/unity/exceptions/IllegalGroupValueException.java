/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when group value is invalid.
 * @author K. Benedyczak
 */
public class IllegalGroupValueException extends RuntimeEngineException
{
	private static final long serialVersionUID = 1L;

	public IllegalGroupValueException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalGroupValueException(String msg)
	{
		super(msg);
	}
}
