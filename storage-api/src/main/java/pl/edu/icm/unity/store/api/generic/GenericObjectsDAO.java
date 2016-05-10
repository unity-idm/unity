/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api.generic;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Engine handling DB operations on the generic objects table on a specified type.
 * The implementation is fully type agnostic by using Java generic type, however the intention is to
 * have trivial extensions of this class with the generic type fixed, in order to present a convenient to use, 
 * injectable API. 
 * <p>
 * It uses {@link GenericEntityHandler} for performing type-specific operations.
 * To maintain database consistency events are produced before all modification operations, so the registered
 * listeners can block the operation.
 * 
 * @author K. Benedyczak
 */
public interface GenericObjectsDAO<T>
{
	boolean exists(String name);
	void assertExist(Collection<String> names);
	T get(String name);
	List<T> getAll();
	List<Map.Entry<T, Date>> getAllWithUpdateTimestamps();
	List<Map.Entry<String, Date>> getAllNamesWithUpdateTimestamps();
	Set<String> getAllNames();
	Map<String, T> getAllAsMap();
	void remove(String name);
	void removeAllNoCheck();
	void update(String current, T newValue);
	void updateTS(String id);
	void insert(String name, T newValue);
}
