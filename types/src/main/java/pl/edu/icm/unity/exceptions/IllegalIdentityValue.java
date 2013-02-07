/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when attribtue value is invalid.
 * @author K. Benedyczak
 */
public class IllegalIdentityValue extends RuntimeEngineException
{
	private static final long serialVersionUID = 1L;

	public IllegalIdentityValue(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public IllegalIdentityValue(String msg)
	{
		super(msg);
	}
}
