/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.base.exceptions.EngineException;

/**
 * Thrown when there is authorization problem
 * @author K. Benedyczak
 */
public class AuthorizationException extends EngineException
{
	private static final long serialVersionUID = 1L;

	public AuthorizationException(String msg, Throwable cause)
	{
		super(msg, cause);
	}

	public AuthorizationException(String msg)
	{
		super(msg);
	}
}
