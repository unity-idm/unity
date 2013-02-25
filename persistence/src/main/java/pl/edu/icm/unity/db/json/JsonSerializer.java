/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

/**
 * Converts objects to/from JSON 
 * @author K. Benedyczak
 * @param <T>
 */
public interface JsonSerializer<T>
{
	public byte[] toJson(T src);
	public void fromJson(byte[] json, T target);
	public Class<?> getSupportedClass();
}
