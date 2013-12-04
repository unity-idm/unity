/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;

/**
 * This class object is returned by authenticator with information about authentication result. 
 * This cover authentication result of a single authenticator, not the combined result of authentication
 * with all authenticators in the set.
 * 
 * @author K. Benedyczak
 */
public class AuthenticationResult
{
	public enum Status {
		/**
		 * There was no input for authenticator
		 */
		notApplicable, 
		
		/**
		 * There was authentication try with the authenticator but it failed
		 */
		deny, 
		
		/**
		 * Can happen only in the case of remote authenticators, when the 
		 * authentication was successful, but the remote principal is not 
		 * registered locally. 
		 */
		unknownRemotePrincipal, 
		
		/**
		 * Everything OK
		 */
		success
	}
	
	private Status status;	
	private RemotelyAuthenticatedContext remoteAuthnContext;
	private AuthenticatedEntity authenticatedEntity;
	private String formForUnknownPrincipal;

	/**
	 * Used by local varificators
	 * @param status
	 * @param authenticatedEntity
	 */
	public AuthenticationResult(Status status, AuthenticatedEntity authenticatedEntity)
	{
		this.status = status;
		this.authenticatedEntity = authenticatedEntity;
	}

	/**
	 * Used by remote verificators
	 * @param status
	 * @param remoteAuthnContext
	 * @param authenticatedEntity
	 */
	public AuthenticationResult(Status status, RemotelyAuthenticatedContext remoteAuthnContext,
			AuthenticatedEntity authenticatedEntity)
	{
		this.status = status;
		this.remoteAuthnContext = remoteAuthnContext;
		this.authenticatedEntity = authenticatedEntity;
	}

	public Status getStatus()
	{
		return status;
	}

	public AuthenticatedEntity getAuthenticatedEntity()
	{
		return authenticatedEntity;
	}

	public RemotelyAuthenticatedContext getRemoteAuthnContext()
	{
		return remoteAuthnContext;
	}

	public String getFormForUnknownPrincipal()
	{
		return formForUnknownPrincipal;
	}

	public void setFormForUnknownPrincipal(String formForUnknownPrincipal)
	{
		this.formForUnknownPrincipal = formForUnknownPrincipal;
	}
}
