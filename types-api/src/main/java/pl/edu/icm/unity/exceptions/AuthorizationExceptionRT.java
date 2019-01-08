/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when there is authorization problem, this is a runtime variant.
 * @author K. Benedyczak
 */
public class AuthorizationExceptionRT extends RuntimeException
{
	public AuthorizationExceptionRT(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public AuthorizationExceptionRT(String msg)
	{
		super(msg);
	}
}
