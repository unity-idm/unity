/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * General purpose interface for objects which can have its state serialized into Json and then read back. 
 * We prefer this to the binary Java serialization.
 * 
 * It is assumed that implementations should have a default constructor.
 * @author K. Benedyczak
 */
public interface JsonSerializable
{
	/**
	 * @return JSON serialized representation
	 */
	public String getSerializedConfiguration();
	
	/**
	 * Initializes object from JSON
	 * @param json
	 */
	public void setSerializedConfiguration(String json);
}
