/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import com.hazelcast.nio.serialization.StreamSerializer;

/**
 * Used to register {@link StreamSerializer}s in Hazelcast configuration.
 * @author K. Benedyczak
 */
public interface SerializerProvider<T> extends StreamSerializer<T>
{
	Class<T> getTypeClass();
}
