/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.hz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.TransactionalMap;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.store.ReferenceAwareDAO;
import pl.edu.icm.unity.store.ReferenceRemovalHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler;
import pl.edu.icm.unity.store.ReferenceUpdateHandler.PlannedUpdateEvent;
import pl.edu.icm.unity.store.api.BasicCRUDDAO;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;

/**
 * Generic BasicCRUDDAO implementation on hazelcast map.
 * @author K. Benedyczak
 */
public abstract class GenericBasicHzCRUD<T> implements BasicCRUDDAO<T>, HzDAO, ReferenceAwareDAO<T>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_DB, GenericBasicHzCRUD.class);

	protected final String STORE_ID;
	protected final String name;
	protected final String rdbmsCounterpartDaoName;
	protected IAtomicLong index;
	private BasicCRUDDAO<T> rdbmsDAO;
	private Set<ReferenceRemovalHandler> deleteHandlers = new HashSet<>();
	private Set<ReferenceUpdateHandler<T>> updateHandlers = new HashSet<>();
	
	public GenericBasicHzCRUD(String storeId, String name, String rdbmsCounterpartDaoName,
			BasicCRUDDAO<T> rdbmsDAO)
	{
		STORE_ID = storeId;
		this.name = name;
		this.rdbmsCounterpartDaoName = rdbmsCounterpartDaoName;
		this.rdbmsDAO = rdbmsDAO;
	}

	@Override
	public void populateFromRDBMS(HazelcastInstance hzInstance)
	{
		log.info("Loading " + name + " from persistent storage");
		index = hzInstance.getAtomicLong(STORE_ID);
		if (getMap().size() != 0)
			throw new IllegalStateException("In-memory data is non empty before loading " + name + 
					", have " + getMap().size() + " entries");

		List<T> all = rdbmsDAO.getAll();
		for (T element: all)
			createNoPropagateToRDBMS(element);
	}

	protected long createNoPropagateToRDBMS(T obj) throws IllegalArgumentException
	{
		TransactionalMap<Long, T> hMap = getMap();
		long key = index.incrementAndGet();
		while (hMap.containsKey(key))
			key = index.incrementAndGet();
		hMap.put(key, obj);
		return key;
	}
	
	@Override
	public long create(T obj) throws IllegalArgumentException
	{
		long key = createNoPropagateToRDBMS(obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"createWithId", key, obj));
		return key;
	}

	@Override
	public List<Long> createList(List<T> entities)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void createWithId(long key, T obj)
	{
		TransactionalMap<Long, T> hMap = getMap();
		if (hMap.containsKey(key))
			throw new IllegalArgumentException(name + " [" + key + "] already exists");
		hMap.put(key, obj);
		index.incrementAndGet();

		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"createWithId", key, obj));
	}
	
	@Override
	public void updateByKey(long id, T obj)
	{
		TransactionalMap<Long, T> hMap = getMap();
		T old = hMap.get(id);
		if (old == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		preUpdateCheck(old, obj);
		firePreUpdate(id, null, obj, old);
		hMap.put(id, obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"updateByKey", id, obj));
	}
	
	@Override
	public long getCount()
	{
		TransactionalMap<Long, T> hMap = getMap();
		return hMap.values().size();
	}
	/**
	 * For extensions
	 * @param old
	 * @param updated
	 */
	protected void preUpdateCheck(T old, T updated)
	{
	}
	
	public T deleteByKeyRet(long id, boolean fireEvent)
	{
		TransactionalMap<Long, T> hMap = getMap();
		T removed = hMap.get(id);
		if (removed == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		if (fireEvent)
			firePreRemove(id, null, removed);
		hMap.remove(id);
		if (fireEvent)
			HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"deleteByKey", id));
		return removed;
	}
	
	@Override
	public void deleteByKey(long id)
	{
		deleteByKeyRet(id, true);
	}

	@Override
	public void deleteAll()
	{
		TransactionalMap<Long, T> hMap = getMap();
		Set<Long> keySet = hMap.keySet();
		for (Long key: keySet)
			deleteByKeyRet(key, true);
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

	protected void firePreUpdate(long modifiedId, String modifiedName, T newVal, T oldVal)
	{
		for (ReferenceUpdateHandler<T> handler: updateHandlers)
			handler.preUpdateCheck(new PlannedUpdateEvent<>(modifiedId, modifiedName, newVal, oldVal));
	}
	
	protected TransactionalMap<Long, T> getMap()
	{
		return HzTransactionTL.getHzContext().getMap(STORE_ID);
	}
}
