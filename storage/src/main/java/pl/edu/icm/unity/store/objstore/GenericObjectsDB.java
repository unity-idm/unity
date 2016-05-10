/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.store.api.generic.GenericObjectsDAO;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;

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
public class GenericObjectsDB<T> implements GenericObjectsDAO<T>
{
	protected GenericEntityHandler<T> handler;
	protected ObjectStoreDAO dbGeneric;
	protected String type;
	protected DependencyNotificationManager notificationManager;
	protected String objectName;
	
	public GenericObjectsDB(GenericEntityHandler<T> handler, ObjectStoreDAO dbGeneric, 
			DependencyNotificationManager notificationManager, Class<T> handledObjectClass,
			String name)
	{
		this.handler = handler;
		this.dbGeneric = dbGeneric;
		this.notificationManager = notificationManager;
		if (!handledObjectClass.equals(this.handler.getModelClass()))
			throw new IllegalArgumentException("Handler and model object are incomatible");
		this.type = handler.getType();
		this.objectName = name;
	}


	public boolean exists(String name)
	{
		return dbGeneric.getObjectByNameType(name, type) != null;
	}
	
	public void assertExist(Collection<String> names)
	{
		Set<String> all = dbGeneric.getNamesOfType(type);
		Set<String> missing = new HashSet<String>();
		for (String name: names)
			if (!all.contains(name))
				missing.add(name);
		if (missing.isEmpty())
			return;
		throw new IllegalArgumentException("The following " + objectName + " are not available: " 
			+ missing.toString());
	}	
	
	public T get(String name)
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(name, type);
		if (raw == null)
			throw new IllegalArgumentException("There is no [" + name + "] " + objectName);
		return handler.fromBlob(raw);
	}
	
	public List<T> getAll()
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type);
		List<T> ret = new ArrayList<>(allRaw.size());
		for (GenericObjectBean raw: allRaw)
			ret.add(handler.fromBlob(raw));
		return ret;
	}

	public List<Map.Entry<T, Date>> getAllWithUpdateTimestamps()
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type);
		List<Map.Entry<T, Date>> ret = new ArrayList<>(allRaw.size());
		
		for (GenericObjectBean raw: allRaw)
			ret.add(new AbstractMap.SimpleEntry<T, Date>(
					handler.fromBlob(raw), raw.getLastUpdate()));
		return ret;
	}

	public List<Map.Entry<String, Date>> getAllNamesWithUpdateTimestamps()
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type);
		List<Map.Entry<String, Date>> ret = new ArrayList<>(allRaw.size());
		
		for (GenericObjectBean raw: allRaw)
			ret.add(new AbstractMap.SimpleEntry<String, Date>(
					raw.getName(), raw.getLastUpdate()));
		return ret;
	}
	public Set<String> getAllNames()
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type);
		Set<String> ret = new HashSet<>(allRaw.size());
		for (GenericObjectBean raw: allRaw)
			ret.add(raw.getName());
		return ret;
	}
	
	public Map<String, T> getAllAsMap()
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type);
		Map<String, T> ret = new HashMap<>(allRaw.size());
		for (GenericObjectBean raw: allRaw)
			ret.put(raw.getName(), handler.fromBlob(raw));
		return ret;
	}
	
	public void remove(String name)
	{
		T removed = get(name);
		notificationManager.firePreRemoveEvent(type, removed);
		dbGeneric.removeObject(name, type);
	}

	public void removeAllNoCheck()
	{
		dbGeneric.removeObjectsByType(type);
	}
	
	public void update(String current, T newValue)
	{
		T updated = get(current);
		notificationManager.firePreUpdateEvent(type, updated, newValue);
		GenericObjectBean blob = handler.toBlob(newValue);
		dbGeneric.updateObject(blob.getName(), blob.getType(), blob.getContents());
	}

	public void updateTS(String id)
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(id, type);
		if (raw == null)
			throw new IllegalArgumentException("There is no [" + id + "] " + objectName);
		dbGeneric.updateObject(id, type, raw.getContents());
	}

	public void insert(String name, T newValue)
	{
		notificationManager.firePreAddEvent(type, newValue);
		GenericObjectBean blob = handler.toBlob(newValue);
		dbGeneric.create(blob);
	}
}
