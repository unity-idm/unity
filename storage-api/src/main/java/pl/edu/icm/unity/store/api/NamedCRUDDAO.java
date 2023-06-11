/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.describedObject.NamedObject;
import pl.edu.icm.unity.store.types.UpdateFlag;

/**
 * DAO with typical CRUD operations for types which are identifiable by string name.
 * @author K. Benedyczak
 */
public interface NamedCRUDDAO<T extends NamedObject> extends BasicCRUDDAO<T>
{	
	void delete(String id);
	
	void updateByNameControlled(String current, T newValue, EnumSet<UpdateFlag> flags);
	
	boolean exists(String id);

	Map<String, T> getAllAsMap();

	T get(String id);
	
	long getKeyForName(String id);
	
	Set<String> getAllNames();

	default void updateControlled(T obj, EnumSet<UpdateFlag> flags)
	{
		updateByNameControlled(obj.getName(), obj, flags);
	}

	default void update(T obj)
	{
		updateByNameControlled(obj.getName(), obj, EnumSet.noneOf(UpdateFlag.class));
	}
	
	default void updateByName(String current, T newValue)
	{
		updateByNameControlled(current, newValue, EnumSet.noneOf(UpdateFlag.class));
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
