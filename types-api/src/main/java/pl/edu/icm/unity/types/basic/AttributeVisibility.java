/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

/**
 * Visibility of an attribute.
 * @author K. Benedyczak
 */
public enum AttributeVisibility
{
	/**
	 * Only available for management interfaces, not for any consumers.
	 */
	local, 
	
	/**
	 * Fully available
	 */
	full;
}
