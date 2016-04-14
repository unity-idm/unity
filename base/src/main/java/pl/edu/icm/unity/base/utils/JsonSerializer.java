/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.utils;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Implementations allow for serializing object to from JSON.
 * @author K. Benedyczak
 */
public interface JsonSerializer<T>
{
	T fromJson(ObjectNode src);
	ObjectNode toJson(T src);
}
