/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * Serialization to/from JSON for Kryo.
 * @author K. Benedyczak
 */
public interface JsonSerializerForKryo<T>
{
	T fromJson(ObjectNode src);
	ObjectNode toJson(T src);
	Class<?> getClazz();
}
