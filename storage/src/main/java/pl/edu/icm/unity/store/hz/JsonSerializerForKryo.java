/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.impl.StorageLimits;


/**
 * Serialization to/from JSON for Kryo.
 * @author K. Benedyczak
 */
public interface JsonSerializerForKryo<T>
{
	T fromJson(ObjectNode src);
	ObjectNode toJson(T src);
	Class<?> getClazz();
	
	//that's bit smelly - not really part of this interface contract, but we don't have any other handy alternative
	//and developing one for this tiny method looks like an overkill
	default void assertSizeLimit(byte [] contents)
	{
		StorageLimits.checkContentsLimit(contents);
	}
}
