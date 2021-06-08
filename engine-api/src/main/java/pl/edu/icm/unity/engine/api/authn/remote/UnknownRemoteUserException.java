/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult;

/**
 * Thrown on authentication problem, when the user is properly authenticated remotely 
 * but is unknown in the local DB (after a complete translation profile execution).
 * 
 * @author K. Benedyczak
 */
public class UnknownRemoteUserException extends AuthenticationException
{
	public UnknownRemoteUserException(String msg, RemoteAuthenticationResult result)
	{
		super(result, msg);
		if (result.getStatus() != Status.unknownRemotePrincipal)
			throw new IllegalArgumentException("Wrong status: " + result.getStatus());
	}

	/**
	 * 
	 * @return registration form for the locally unknown user, or null if not defined.
	 */
	public String getFormForUser()
	{
		return getResult().asRemote().getUnknownRemotePrincipalResult().formForUnknownPrincipal;
	}

	public RemotelyAuthenticatedPrincipal getRemoteContext()
	{
		return getResult().asRemote().getRemotelyAuthenticatedPrincipal();
	}
	
	public RemoteAuthenticationResult getResult()
	{
		return super.getResult().asRemote();
	}
}
