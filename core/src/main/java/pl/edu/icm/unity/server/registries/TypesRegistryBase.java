/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.registries;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.exceptions.IllegalTypeException;

/**
 * Maintains a simple registry of some implementations.
 * 
 * @author K. Benedyczak
 */
public abstract class TypesRegistryBase<T>
{
	private Map<String, T> elements;
	
	public TypesRegistryBase(List<T> typeElements)
	{
		this.elements = new HashMap<String, T>(10);
		for (T idDef: typeElements)
			this.elements.put(getId(idDef), idDef);
	}
	
	public T getByName(String name) throws IllegalTypeException
	{
		T ret = elements.get(name);
		if (ret == null)
			throw new IllegalTypeException("Type " + name + " is not supported");
		return ret;
	}
	
	public Collection<T> getAll()
	{
		return elements.values();
	}
	
	protected abstract String getId(T from);
}
