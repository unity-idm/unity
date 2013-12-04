/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote;

import pl.edu.icm.unity.exceptions.AuthenticationException;

/**
 * Thrown on authentication problem, when the user is properly authenticated remotely 
 * but is unknown in the local DB (after a complete translation profile execution).
 * 
 * @author K. Benedyczak
 */
public class UnknownRemoteUserException extends AuthenticationException
{
	private static final long serialVersionUID = 1L;

	private String form;
	private RemotelyAuthenticatedContext remoteContext;
	
	public UnknownRemoteUserException(String msg, String form, RemotelyAuthenticatedContext remoteContext)
	{
		super(msg);
		this.form = form;
		this.remoteContext = remoteContext;
	}

	/**
	 * 
	 * @return registration form for the locally unknown user, or null if not defined.
	 */
	public String getFormForUser()
	{
		return form;
	}

	public RemotelyAuthenticatedContext getRemoteContext()
	{
		return remoteContext;
	}
}
