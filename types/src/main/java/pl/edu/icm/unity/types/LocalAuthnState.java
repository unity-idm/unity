/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * State of local authN 
 * @author K. Benedyczak
 */
public enum LocalAuthnState
{
	/**
	 * Local authn is diasbled - at least one {@link LocalAuthnConfirmation} has invalid status.
	 */
	disabled,
	
	/**
	 * Local authn can not be done using the previous configuration and previous secrets. However
	 * the only operation which is possible is after authentication is update of secrets.
	 */
	outdated,
	
	/**
	 * Everything is fine
	 */
	correct
}
