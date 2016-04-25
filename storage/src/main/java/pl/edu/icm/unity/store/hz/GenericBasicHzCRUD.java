/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.ArrayList;
import java.util.List;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.TransactionalMap;

import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSMutationEvent;

/**
 * Generic BasicCRUDDAO implementation on hazelcast map.
 * @author K. Benedyczak
 */
public abstract class GenericBasicHzCRUD<T> implements BasicCRUDDAO<T>
{
	protected final String STORE_ID;
	protected final String name;
	protected final String rdbmsCounterpartDaoName;
	private IAtomicLong index;
	
	public GenericBasicHzCRUD(String storeId, String name, String rdbmsCounterpartDaoName)
	{
		STORE_ID = storeId;
		this.name = name;
		this.rdbmsCounterpartDaoName = rdbmsCounterpartDaoName;
	}

	public void initHazelcast(BasicCRUDDAO<T> rdbmsDAO, HazelcastInstance hzInstance)
	{
		index = hzInstance.getAtomicLong(STORE_ID);
		List<T> all = rdbmsDAO.getAll();
		for (T element: all)
			create(element);
	}
	
	@Override
	public long create(T obj) throws IllegalArgumentException
	{
		TransactionalMap<Long, T> hMap = getMap();
		long key = index.incrementAndGet();
		if (hMap.containsKey(key))
			throw new IllegalArgumentException(name + " [" + key + "] already exists");
		hMap.put(key, obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, "create", obj));
		return key;
	}
	
	@Override
	public void updateByKey(long id, T obj)
	{
		TransactionalMap<Long, T> hMap = getMap();
		if (!hMap.containsKey(id))
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		hMap.put(id, obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, "update", obj));
	}

	@Override
	public void deleteByKey(long id)
	{
		TransactionalMap<Long, T> hMap = getMap();
		T removed = hMap.remove(id);
		if (removed == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, "deleteByKey", id));
	}

	@Override
	public T getByKey(long id)
	{
		TransactionalMap<Long, T> hMap = getMap();
		T entry = hMap.get(id);
		if (entry == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		return entry;
	}

	@Override
	public List<T> getAll()
	{
		TransactionalMap<Long, T> hMap = getMap();
		List<T> ret = new ArrayList<>();
		ret.addAll(hMap.values());
		return ret;
	}
	
	protected TransactionalMap<Long, T> getMap()
	{
		return HzTransactionTL.getHzContext().getMap(STORE_ID);
	}
}
