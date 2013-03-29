/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown on authentication problem
 * @author K. Benedyczak
 */
public class AuthenticationException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public AuthenticationException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public AuthenticationException(String msg)
	{
		super(msg);
	}

}
