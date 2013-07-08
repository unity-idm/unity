/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Overall entity status.
 * 
 * @author K. Benedyczak
 */
public enum EntityState
{
	/**
	 * No restrictions.
	 */
	valid,
	
	/**
	 * The entity is fully valid, but it can not authenticate itself.
	 * It is still possible to query for entity's attributes etc.
	 */
	authenticationDisabled,
	
	/**
	 * The entity is disabled. It is not possible to authenticate as this entity.
	 * What is more it is only possible to get the attributes of the entity using the privileged 
	 * getAllAttribtues operation. Therefore such entity can not be used as a subject 
	 * of SAML queries, etc. All other management operations are still possible. It is also possible to 
	 * get other information about the entity, assuming that authZ level permits.
	 */
	disabled
}
