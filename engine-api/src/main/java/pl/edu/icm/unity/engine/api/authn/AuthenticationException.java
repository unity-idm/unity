/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Thrown on authentication problem.
 * @author K. Benedyczak
 */
public class AuthenticationException extends EngineException
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
