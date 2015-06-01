/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;

/**
 * Thrown on authentication problem, when the user is properly authenticated remotely 
 * but is unknown in the local DB (after a complete translation profile execution).
 * 
 * @author K. Benedyczak
 */
public class UnknownRemoteUserException extends AuthenticationException
{
	private static final long serialVersionUID = 1L;

	public UnknownRemoteUserException(String msg, AuthenticationResult result)
	{
		super(result, msg);
	}

	/**
	 * 
	 * @return registration form for the locally unknown user, or null if not defined.
	 */
	public String getFormForUser()
	{
		return getResult().getFormForUnknownPrincipal();
	}

	public RemotelyAuthenticatedContext getRemoteContext()
	{
		return getResult().getRemoteAuthnContext();
	}
}
