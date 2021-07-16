/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

/**
 * Thrown on authentication problem.
 * @author K. Benedyczak
 */
public class RemoteAuthenticationException extends AuthenticationException
{
	private RemoteAuthenticationResult result;

	public RemoteAuthenticationException(String msg)
	{
		super(msg);
		this.result = RemoteAuthenticationResult.failed();
	}

	public RemoteAuthenticationException(String msg, Exception cause)
	{
		super(msg, cause);
		this.result = RemoteAuthenticationResult.failed();
	}

	public RemoteAuthenticationResult getResult()
	{
		return result;
	}
}
