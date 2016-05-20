/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.types.NamedObject;

/**
 * DAO with typical CRUD operations for types which are identifiable by string name.
 * @author K. Benedyczak
 */
public interface NamedCRUDDAO<T extends NamedObject> extends BasicCRUDDAO<T>
{	
	void delete(String id);
	
	void updateByName(String current, T newValue);
	
	boolean exists(String id);

	Map<String, T> getAllAsMap();

	T get(String id);
	
	long getKeyForName(String id);
	
	Set<String> getAllNames();

	default void update(T obj)
	{
		updateByName(obj.getName(), obj);
	}

	default void assertExist(Collection<String> names)
	{
		Set<String> allNames = getAllNames();
		Set<String> missing = Sets.difference(new HashSet<>(names), allNames);
		if (missing.isEmpty())
			return;
		throw new IllegalArgumentException("The following objects are not available: " 
				+ missing.toString());		
	}
}
