/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * General purpose interface, useful for extending. Defines methods to get object 
 * name and description.
 * @author K. Benedyczak
 */
public interface DescribedObject
{
	/**
	 * @return human readable name of the object. Must be unique for the object class.
	 */
	public String getName();
	
	/**
	 * @return human readable description of the object.
	 */
	public String getDescription();
}
