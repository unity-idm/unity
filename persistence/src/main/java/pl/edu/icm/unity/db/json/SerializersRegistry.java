/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.RuntimeEngineException;

/**
 * Allows to retrieve Json serializers.
 * 
 * @author K. Benedyczak
 */
@Component
public class SerializersRegistry
{
	private Map<Class<?>, JsonSerializer<?>> serializers;

	@Autowired
	public SerializersRegistry(List<JsonSerializer<?>> serializers)
	{
		this.serializers = new HashMap<Class<?>, JsonSerializer<?>>(serializers.size());
		for (JsonSerializer<?> s: serializers)
		{
			this.serializers.put(s.getSupportedClass(), s);
		}
	}
	
	public <T> JsonSerializer<T> getSerializer(Class<T> ofClass)
	{
		@SuppressWarnings("unchecked")
		JsonSerializer<T> ret = (JsonSerializer<T>) this.serializers.get(ofClass);
		if (ret == null)
			throw new RuntimeEngineException("No serializer for the " + ofClass + " class is available.");
		return ret;
	}
}
