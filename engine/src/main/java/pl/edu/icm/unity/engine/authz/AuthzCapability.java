/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authz;

/**
 * Each operation requires one or more capabilities to be run. In principle all operations should require only
 * one capability.
 * 
 * @author K. Benedyczak
 */
public enum AuthzCapability {
	/**
	 * System modifications: clearing of the database, credential, authenticators and endpoints management. 
	 */
	maintenance, 

	/**
	 * Modification of identities, entities
	 */
	identityModify,

	/**
	 * Modification of credentials
	 */
	credentialModify,

	/**
	 * Modification of groups
	 */
	groupModify,

	/**
	 * Modification of attributes
	 */
	attributeModify,

	/**
	 * Reading of hidden (local) attributes
	 */
	readHidden,
	
	/**
	 * Reading of groups, identities, entities and attributes
	 */
	read,

	/**
	 * Reading of system information, not related to the actual contents: endpoints, authenticators, 
	 * credential definitions, available attribute syntaxes and identity types.
	 */
	readInfo
}