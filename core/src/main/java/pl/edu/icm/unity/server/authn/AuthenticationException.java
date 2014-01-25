/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;

/**
 * Thrown on authentication problem.
 * @author K. Benedyczak
 */
public class AuthenticationException extends EngineException
{
	private static final long serialVersionUID = 1L;
	private AuthenticationResult result;

	/**
	 * 
	 * @param result authentication result - should be failed, not successful.
	 * @param msg
	 * @param cause
	 */
	public AuthenticationException(AuthenticationResult result, String msg, Throwable cause)
	{
		super(msg, cause);
		this.result = result;
	}

	public AuthenticationException(AuthenticationResult result, String msg)
	{
		super(msg);
		this.result = result;
	}

	public AuthenticationException(String msg)
	{
		super(msg);
		this.result = new AuthenticationResult(Status.deny, null);
	}

	public AuthenticationException(String msg, Exception cause)
	{
		super(msg, cause);
		this.result = new AuthenticationResult(Status.deny, null);
	}

	public AuthenticationResult getResult()
	{
		return result;
	}
}
