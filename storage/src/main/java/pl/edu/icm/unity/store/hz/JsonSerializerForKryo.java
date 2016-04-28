/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import pl.edu.icm.unity.base.utils.JsonSerializer;

/**
 * Nearly a marker interface: Kryo uses all implementation beans for serialization.
 * @author K. Benedyczak
 */
public interface JsonSerializerForKryo<T> extends JsonSerializer<T>
{
	Class<?> getClazz();
}
