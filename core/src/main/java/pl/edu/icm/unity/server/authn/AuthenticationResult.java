/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

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
	//TODO add data about remote authenticated principal
	private AuthenticatedEntity authenticatedEntity;

	public AuthenticationResult(Status status, AuthenticatedEntity authenticatedEntity)
	{
		this.status = status;
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
	
	
}
