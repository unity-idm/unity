/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * General purpose interface, useful for extending. Defines methods to get object 
 * id, name and description.
 * @author K. Benedyczak
 */
public interface DescribedObject
{
	/**
	 * @return id of the object, intended to be unique for the object class and used internally.
	 */
	public String getId();
	
	/**
	 * @return human readable name of the object.
	 */
	public String getName();
	
	/**
	 * @return human readable description of the object.
	 */
	public String getDescription();
}
