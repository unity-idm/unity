/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.core.TransactionalMap;

import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.hz.GenericBasicHzCRUD;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.types.EntityInformation;


/**
 * Hazelcast implementation of entity store.
 * Note on creation: the methods {@link #createNoPropagateToRDBMS(EntityInformation)} (internal) and
 * {@link #createWithId(EntityInformation)} preserves the id given in input. The {@link #create(EntityInformation)} 
 * invents a new id.  
 * @author K. Benedyczak
 */
@Repository(EntityHzStore.STORE_ID)
public class EntityHzStore extends GenericBasicHzCRUD<EntityInformation> implements EntityDAO
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Autowired
	public EntityHzStore(EntityRDBMSStore rdbmsDAO)
	{
		super(STORE_ID, NAME, EntityRDBMSStore.BEAN, rdbmsDAO);
	}
	
	@Override
	protected long createNoPropagateToRDBMS(EntityInformation obj) throws IllegalArgumentException
	{
		TransactionalMap<Long, EntityInformation> hMap = getMap();
		long key = obj.getId();
		if (hMap.containsKey(key))
			throw new IllegalArgumentException(name + " [" + key + "] already exists");
		hMap.put(key, obj);
		index.incrementAndGet();
		return key;
	}

	@Override
	public long create(EntityInformation obj) throws IllegalArgumentException
	{
		TransactionalMap<Long, EntityInformation> hMap = getMap();
		long key = index.incrementAndGet();
		while (hMap.containsKey(key))
			key = index.incrementAndGet();
		obj.setId(key);
		hMap.put(key, obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"createWithId", obj));
		return key;
	}
	
	@Override
	public void createWithId(EntityInformation obj)
	{
		createNoPropagateToRDBMS(obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"createWithId", obj));
	}
}
