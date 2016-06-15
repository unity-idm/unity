/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.utils;

/**
 * This interface is enforced for some types of objects which are instantiated otherwise 
 * and later on configured with some text configuration. Useful for types which allow for different
 * configuration languages then JSON, as property format. 
 * @author K. Benedyczak
 */
public interface StringConfigurable
{
	/**
	 * @return serialized configuration of this object
	 */
	String getSerializedConfiguration();
	
	/**
	 * Sets configuration of this object
	 * @param jconfig
	 */
	void setSerializedConfiguration(String config);
}
