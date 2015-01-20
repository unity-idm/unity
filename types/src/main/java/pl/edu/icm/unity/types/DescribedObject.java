/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * General purpose interface, useful for extending. Defines methods to get object 
 * name and description. This version is useful for objects providing a fixed, implementation defined 
 * name and description. In future the description type should be changed to {@link I18nString} so that a 
 * localized value is returned, however the intention is that the description returned by this 
 * interface is aimed at admin only.
 * 
 * @author K. Benedyczak
 */
public interface DescribedObject extends NamedObject
{
	/**
	 * @return human readable description of the object.
	 */
	public String getDescription();
}
