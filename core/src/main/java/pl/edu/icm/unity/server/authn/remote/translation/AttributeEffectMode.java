/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn.remote.translation;

/**
 * Defines how a mapped attribute should be handled. 
 * @author K. Benedyczak
 */
public enum AttributeEffectMode
{
	/**
	 * Local attribute will be created if doesn't exists. If it does, then the values will be set to the 
	 * new value. 
	 */
	CREATE_OR_UPDATE, 
	
	/**
	 * If a local attribute exists it will be updated. If it doesn't nothing will be done.
	 */
	UPDATE_ONLY, 
	
	
	/**
	 * If the local attribute doesn't exist it will be created. If it does nothing will be done.
	 */
	CREATE_ONLY
}
