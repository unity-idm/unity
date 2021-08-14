/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

/**
 * Thrown on authentication problem.
 * @author K. Benedyczak
 */
public class AuthenticationException extends Exception
{
	private AuthenticationResult result;

	public AuthenticationException(AuthenticationResult result, String msg)
	{
		super(msg);
		this.result = result;
	}

	public AuthenticationException(String msg)
	{
		super(msg);
		this.result = LocalAuthenticationResult.failed();
	}

	public AuthenticationException(String msg, Exception cause)
	{
		super(msg, cause);
		this.result = LocalAuthenticationResult.failed(cause);
	}

	public AuthenticationResult getResult()
	{
		return result;
	}
}
