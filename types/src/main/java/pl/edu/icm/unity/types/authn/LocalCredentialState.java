/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

/**
 * State of the local credential initialization for an entity. This is 
 * pertaining to a single credential - entity might have more then one.
 * @author K. Benedyczak
 */
public enum LocalCredentialState
{
	/**
	 * the credential is fine
	 */
	correct,
	
	/**
	 * the credential is not defined
	 */
	notSet,
	
	/**
	 * the credential is defined but is invalid. E.g. it is not fullfilling the new restrictions or
	 * is outdated.
	 */
	outdated
}
