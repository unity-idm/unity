/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.exceptions;

/**
 * Thrown when there is authorization problem
 * @author K. Benedyczak
 */
public class RuntimeAuthorizationException extends RuntimeEngineException
{
	private static final long serialVersionUID = 1L;

	public RuntimeAuthorizationException(String msg, EngineException cause)
	{
		super(msg, cause);
	}

	public RuntimeAuthorizationException(String msg)
	{
		super(msg);
	}

	public RuntimeAuthorizationException(AuthorizationException exception)
	{
		super(exception);
	}
}
