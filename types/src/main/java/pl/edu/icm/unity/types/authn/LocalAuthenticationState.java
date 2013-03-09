/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

/**
 * Describes the overall state of credentials set of an entity.
 * 
 * @author K. Benedyczak
 */
public enum LocalAuthenticationState
{
	/**
	 * Regardless of the status of the credentials, the entity won't be able to authenticate itself.
	 */
	disabled,
	
	/**
	 * Authentication can be performed using the existing credentials, out of which at least one is 
	 * outdated. After authentication, the only allowed operation is to update the credential. 
	 * When all the credentials are updated and have the correct state, then state of the 
	 * authentication is automatically moved to valid. 
	 */
	outdated,
	
	/**
	 * All the required credentials are in the correct state, and the authentication can be performed 
	 * in the normal way.
	 */
	valid
}
