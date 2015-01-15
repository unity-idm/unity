/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Defines method to get object's name.
 * 
 * @author K. Benedyczak
 */
public interface NamedObject
{
	/**
	 * @return human readable name of the object. Must be unique for the object class.
	 */
	public String getName();
}
