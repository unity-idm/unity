/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.HashMap;
import java.util.Map;

import com.hazelcast.core.TransactionalMap;

import pl.edu.icm.unity.store.api.CRUDDAO;
import pl.edu.icm.unity.store.tx.TransactionTL;

/**
 * Generic CRUD implementation on hazelcast map.
 * @author K. Benedyczak
 */
public abstract class GenericHzCRUD<T> implements CRUDDAO<T>
{
	private final String STORE_ID;
	private final String name; 
	
	public GenericHzCRUD(String storeId, String name)
	{
		STORE_ID = storeId;
		this.name = name;
	}

	protected abstract String getKey(T obj);

	@Override
	public void create(T obj) throws IllegalArgumentException
	{
		TransactionalMap<String, T> hMap = getMap();
		String key = getKey(obj);
		if (hMap.containsKey(key))
			throw new IllegalArgumentException(name + " [" + key + "] already exists");
		hMap.put(key, obj);
	}
	
	@Override
	public void update(T obj)
	{
		TransactionalMap<String, T> hMap = getMap();
		String key = getKey(obj);
		if (!hMap.containsKey(key))
			throw new IllegalArgumentException(name + " [" + key + "] does not exists");
		hMap.put(key, obj);
	}

	@Override
	public void delete(String id)
	{
		getMap().remove(id);
	}

	@Override
	public T get(String id)
	{
		T obj = getMap().get(id);
		if (obj == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		return obj;
	}

	@Override
	public boolean exists(String id)
	{
		T obj = getMap().get(id);
		return obj != null;
	}
	
	@Override
	public Map<String, T> getAsMap()
	{
		TransactionalMap<String, T> hMap = getMap();
		Map<String, T> ret = new HashMap<>();
		for (String key: hMap.keySet())
			ret.put(key, hMap.get(key));
		return ret;
	}

	protected TransactionalMap<String, T> getMap()
	{
		return TransactionTL.getHzContext().getMap(STORE_ID);
	}
}
