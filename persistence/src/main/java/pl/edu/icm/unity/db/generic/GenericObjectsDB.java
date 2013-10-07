/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.generic;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;

import pl.edu.icm.unity.db.DBGeneric;
import pl.edu.icm.unity.db.model.GenericObjectBean;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;

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
public class GenericObjectsDB<T>
{
	protected GenericEntityHandler<T> handler;
	protected DBGeneric dbGeneric;
	protected String type;
	protected DependencyNotificationManager notificationManager;
	protected String objectName;
	
	public GenericObjectsDB(GenericEntityHandler<T> handler, DBGeneric dbGeneric, 
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


	public boolean exists(String name, SqlSession sql) throws EngineException
	{
		return dbGeneric.getObjectByNameType(name, type, sql) != null;
	}
	
	public void assertExist(Collection<String> names, SqlSession sql) throws EngineException
	{
		Set<String> all = dbGeneric.getNamesOfType(type, sql);
		Set<String> missing = new HashSet<String>();
		for (String name: names)
			if (!all.contains(name))
				missing.add(name);
		if (missing.isEmpty())
			return;
		throw new WrongArgumentException("The following " + objectName + "s are not available: " 
			+ missing.toString());
	}	
	
	public T get(String name, SqlSession sql) throws EngineException
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(name, type, sql);
		if (raw == null)
			throw new WrongArgumentException("There is no " + name + " " + objectName);
		return handler.fromBlob(raw, sql);
	}
	
	public List<T> getAll(SqlSession sql) throws EngineException
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type, sql);
		List<T> ret = new ArrayList<>(allRaw.size());
		for (GenericObjectBean raw: allRaw)
			ret.add(handler.fromBlob(raw, sql));
		return ret;
	}

	public List<Map.Entry<T, Date>> getAllWithUpdateTimestamps(SqlSession sql) throws EngineException
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type, sql);
		List<Map.Entry<T, Date>> ret = new ArrayList<>(allRaw.size());
		
		for (GenericObjectBean raw: allRaw)
			ret.add(new AbstractMap.SimpleEntry<T, Date>(
					handler.fromBlob(raw, sql), raw.getLastUpdate()));
		return ret;
	}

	public List<Map.Entry<String, Date>> getAllNamesWithUpdateTimestamps(SqlSession sql) throws EngineException
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type, sql);
		List<Map.Entry<String, Date>> ret = new ArrayList<>(allRaw.size());
		
		for (GenericObjectBean raw: allRaw)
			ret.add(new AbstractMap.SimpleEntry<String, Date>(
					raw.getName(), raw.getLastUpdate()));
		return ret;
	}
	public Set<String> getAllNames(SqlSession sql) throws EngineException
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type, sql);
		Set<String> ret = new HashSet<>(allRaw.size());
		for (GenericObjectBean raw: allRaw)
			ret.add(raw.getName());
		return ret;
	}
	
	public Map<String, T> getAllAsMap(SqlSession sql) throws EngineException
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type, sql);
		Map<String, T> ret = new HashMap<>(allRaw.size());
		for (GenericObjectBean raw: allRaw)
			ret.put(raw.getName(), handler.fromBlob(raw, sql));
		return ret;
	}
	
	public void remove(String name, SqlSession sql) throws EngineException
	{
		T removed = get(name, sql);
		notificationManager.firePreRemoveEvent(type, removed, sql);
		dbGeneric.removeObject(name, type, sql);
	}

	public void removeAllNoCheck(SqlSession sql) throws EngineException
	{
		dbGeneric.removeObjectsByType(type, sql);
	}
	
	public void update(String current, T newValue, SqlSession sql) throws EngineException
	{
		T updated = get(current, sql);
		notificationManager.firePreUpdateEvent(type, updated, newValue, sql);
		GenericObjectBean blob = handler.toBlob(newValue, sql);
		dbGeneric.updateObject(blob.getName(), blob.getType(), blob.getContents(), sql);
	}

	public void updateTS(String id, SqlSession sql) throws EngineException
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(id, type, sql);
		if (raw == null)
			throw new WrongArgumentException("There is no " + id + " " + objectName);
		dbGeneric.updateObject(id, type, raw.getContents(), sql);
	}

	public void insert(String name, T newValue, SqlSession sql) throws EngineException
	{
		notificationManager.firePreAddEvent(type, newValue, sql);
		GenericObjectBean blob = handler.toBlob(newValue, sql);
		dbGeneric.addObject(blob.getName(), blob.getType(), blob.getSubType(), blob.getContents(), sql);
	}
}
