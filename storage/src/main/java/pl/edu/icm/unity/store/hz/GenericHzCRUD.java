/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hazelcast.core.TransactionalMap;

import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSMutationEvent;

/**
 * Generic CRUD implementation on hazelcast map.
 * @author K. Benedyczak
 */
public abstract class GenericHzCRUD<T> implements BasicCRUDDAO<T>
{
	private final String STORE_ID;
	private final String name;
	private final String rdbmsCounterpartDaoName; 
	
	public GenericHzCRUD(String storeId, String name, String rdbmsCounterpartDaoName)
	{
		STORE_ID = storeId;
		this.name = name;
		this.rdbmsCounterpartDaoName = rdbmsCounterpartDaoName;
	}

	protected abstract String getKey(T obj);

	public void initHazelcast(BasicCRUDDAO<T> rdbmsDAO)
	{
		List<T> all = rdbmsDAO.getAll();
		for (T element: all)
			create(element);
	}
	
	@Override
	public void create(T obj) throws IllegalArgumentException
	{
		TransactionalMap<String, T> hMap = getMap();
		String key = getKey(obj);
		if (hMap.containsKey(key))
			throw new IllegalArgumentException(name + " [" + key + "] already exists");
		hMap.put(key, obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, "create", obj));
	}
	
	@Override
	public void update(T obj)
	{
		TransactionalMap<String, T> hMap = getMap();
		String key = getKey(obj);
		if (!hMap.containsKey(key))
			throw new IllegalArgumentException(name + " [" + key + "] does not exists");
		hMap.put(key, obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, "update", obj));
	}

	@Override
	public void delete(String id)
	{
		T removed = getMap().remove(id);
		if (removed == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, "delete", id));
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

	@Override
	public List<T> getAll()
	{
		TransactionalMap<String, T> hMap = getMap();
		List<T> ret = new ArrayList<>();
		for (String key: hMap.keySet())
			ret.add(hMap.get(key));
		return ret;
	}
	
	protected TransactionalMap<String, T> getMap()
	{
		return HzTransactionTL.getHzContext().getMap(STORE_ID);
	}
}
