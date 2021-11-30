/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.store.ReferenceAwareDAO;
import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler.PlannedUpdateEvent;
import pl.edu.icm.unity.store.api.generic.NamedCRUDDAOWithTS;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.impl.objstore.ObjectStoreDAO;
import pl.edu.icm.unity.store.types.UpdateFlag;
import pl.edu.icm.unity.types.NamedObject;

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
public class GenericObjectsDAOImpl<T extends NamedObject> implements NamedCRUDDAOWithTS<T>, ReferenceAwareDAO<T>
{
	protected GenericEntityHandler<T> handler;
	protected ObjectStoreDAO dbGeneric;
	protected String type;
	protected String objectName;
	private Set<ReferenceRemovalHandler> deleteHandlers = new HashSet<>();
	private Set<ReferenceUpdateHandler<T>> updateHandlers = new HashSet<>();
	
	public GenericObjectsDAOImpl(GenericEntityHandler<T> handler, ObjectStoreDAO dbGeneric, 
			Class<T> handledObjectClass, String name)
	{
		this.handler = handler;
		this.dbGeneric = dbGeneric;
		if (!handledObjectClass.equals(this.handler.getModelClass()))
			throw new IllegalArgumentException("Handler and model object are incomatible");
		this.type = handler.getType();
		this.objectName = name;
	}


	@Override
	public boolean exists(String name)
	{
		return dbGeneric.getObjectByNameType(name, type) != null;
	}
	
	@Override
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
	
	@Override
	public T get(String name)
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(name, type);
		if (raw == null)
			throw new IllegalArgumentException("There is no [" + name + "] " + objectName);
		return handler.fromBlob(raw);
	}
	
	@Override
	public List<T> getAll()
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type);
		List<T> ret = new ArrayList<>(allRaw.size());
		for (GenericObjectBean raw: allRaw)
			ret.add(handler.fromBlob(raw));
		return ret;
	}

	@Override
	public List<Map.Entry<T, Date>> getAllWithUpdateTimestamps()
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type);
		List<Map.Entry<T, Date>> ret = new ArrayList<>(allRaw.size());
		
		for (GenericObjectBean raw: allRaw)
			ret.add(new AbstractMap.SimpleEntry<T, Date>(
					handler.fromBlob(raw), raw.getLastUpdate()));
		return ret;
	}

	@Override
	public List<Map.Entry<String, Date>> getAllNamesWithUpdateTimestamps()
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type);
		List<Map.Entry<String, Date>> ret = new ArrayList<>(allRaw.size());
		
		for (GenericObjectBean raw: allRaw)
			ret.add(new AbstractMap.SimpleEntry<String, Date>(
					raw.getName(), raw.getLastUpdate()));
		return ret;
	}

	@Override
	public Date getUpdateTimestamp(String name)
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(name, type);
		if (raw == null)
			throw new IllegalArgumentException("There is no [" + name + "] " + objectName);
		return raw.getLastUpdate();
	}
	
	@Override
	public Set<String> getAllNames()
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type);
		Set<String> ret = new HashSet<>(allRaw.size());
		for (GenericObjectBean raw: allRaw)
			ret.add(raw.getName());
		return ret;
	}
	
	@Override
	public Map<String, T> getAllAsMap()
	{
		List<GenericObjectBean> allRaw = dbGeneric.getObjectsOfType(type);
		Map<String, T> ret = new HashMap<>(allRaw.size());
		for (GenericObjectBean raw: allRaw)
			ret.put(raw.getName(), handler.fromBlob(raw));
		return ret;
	}
	
	@Override
	public void delete(String name)
	{
		delete(name, false);
	}
	
	protected void delete(String name, boolean ignoreDependencyChecking)
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(name, type);
		if (raw == null)
			throw new IllegalArgumentException("There is no [" + name + "] " + objectName);
		T removed = handler.fromBlob(raw);
		if (!ignoreDependencyChecking)
		{
			firePreRemove(raw.getId(), name, removed);
		}
		dbGeneric.removeObject(name, type);
	}

	@Override
	public void deleteAll()
	{
		dbGeneric.removeObjectsByType(type);
	}
	
	@Override
	public void updateByNameControlled(String current, T newValue, EnumSet<UpdateFlag> flags)
	{
		StorageLimits.checkNameLimit(newValue.getName());
		GenericObjectBean raw = dbGeneric.getObjectByNameType(current, type);
		if (raw == null)
			throw new IllegalArgumentException("There is no [" + current + "] " + objectName);
		T updated = handler.fromBlob(raw);

		firePreUpdate(raw.getId(), current, newValue, updated, flags);
		
		GenericObjectBean blob = handler.toBlob(newValue);
		blob.setLastUpdate(new Date());
		dbGeneric.updateObject(current, blob.getType(), blob);
	}

	@Override
	public void updateByKey(long id, T obj)
	{
		StorageLimits.checkNameLimit(obj.getName());
		GenericObjectBean raw = handler.toBlob(obj);
		raw.setLastUpdate(new Date());
		dbGeneric.updateByKey(id, raw);
	}

	@Override
	public void updateTS(String id)
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(id, type);
		if (raw == null)
			throw new IllegalArgumentException("There is no [" + id + "] " + objectName);
		raw.setLastUpdate(new Date());
		dbGeneric.updateObject(id, type, raw);
	}

	@Override
	public long create(T newValue)
	{
		return createWithTS(newValue, new Date());
	}

	@Override
	public long createWithTS(T newValue, Date updatTS)
	{
		StorageLimits.checkNameLimit(newValue.getName());
		GenericObjectBean blob = handler.toBlob(newValue);
		blob.setLastUpdate(updatTS);
		if (exists(newValue.getName()))
			throw new IllegalArgumentException("The [" + newValue.getName() + "] " + objectName +
					" already exists");
		return dbGeneric.create(blob);
	}

	@Override
	public void createWithId(long id, T obj)
	{
		GenericObjectBean blob = handler.toBlob(obj);
		blob.setLastUpdate(new Date());
		if (exists(obj.getName()))
			throw new IllegalArgumentException("The [" + obj.getName() + "] " + objectName +
					" already exists");
		dbGeneric.createWithId(id, blob);
	}

	@Override
	public List<Long> createList(List<T> objs)
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void deleteByKey(long id)
	{
		dbGeneric.deleteByKey(id);
	}

	@Override
	public T getByKey(long id)
	{
		GenericObjectBean raw = dbGeneric.getByKey(id);
		return handler.fromBlob(raw);
	}

	@Override
	public long getKeyForName(String id)
	{
		GenericObjectBean raw = dbGeneric.getObjectByNameType(id, type);
		if (raw == null)
			throw new IllegalArgumentException("There is no [" + id + "] " + objectName);
		return raw.getId();
	}
	
	@Override
	public long getCount()
	{
		return dbGeneric.getCountByType(type);
	}
	
	@Override
	public void addRemovalHandler(ReferenceRemovalHandler handler)
	{
		deleteHandlers.add(handler);
	}
	
	@Override
	public void addUpdateHandler(ReferenceUpdateHandler<T> handler)
	{
		updateHandlers.add(handler);
	}

	protected void firePreRemove(long modifiedId, String modifiedName, T removed)
	{
		for (ReferenceRemovalHandler handler: deleteHandlers)
			handler.preRemoveCheck(modifiedId, modifiedName);
	}

	protected void firePreUpdate(long modifiedId, String modifiedName, T newVal, T oldVal,
			EnumSet<UpdateFlag> updateFlags)
	{
		PlannedUpdateEvent<T> updateEvent = new PlannedUpdateEvent<>(modifiedId, modifiedName, newVal, 
				oldVal, updateFlags);
		for (ReferenceUpdateHandler<T> handler: updateHandlers)
			handler.preUpdateCheck(updateEvent);
	}
}
