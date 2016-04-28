/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.entities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.EntityDAO;
import pl.edu.icm.unity.store.api.StoredEntity;
import pl.edu.icm.unity.store.hz.GenericBasicHzCRUD;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSMutationEvent;

import com.hazelcast.core.TransactionalMap;


/**
 * Hazelcast implementation of entity store.
 * 
 * @author K. Benedyczak
 */
@Repository(EntityHzStore.STORE_ID)
public class EntityHzStore extends GenericBasicHzCRUD<StoredEntity> implements EntityDAO
{
	public static final String STORE_ID = DAO_ID + "hz";
	private static final String NAME = "entity";

	@Autowired
	public EntityHzStore(EntityRDBMSStore rdbmsDAO)
	{
		super(STORE_ID, NAME, EntityRDBMSStore.BEAN, rdbmsDAO);
	}
	
	@Override
	public long create(StoredEntity obj) throws IllegalArgumentException
	{
		long ret = super.create(obj);
		obj.setId(ret);
		return ret;
	}

	@Override
	public void createWithId(StoredEntity obj)
	{
		TransactionalMap<Long, StoredEntity> hMap = getMap();
		long key = obj.getId();
		if (hMap.containsKey(key))
			throw new IllegalArgumentException(name + " [" + key + "] already exists");
		hMap.put(key, obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, 
				"createWithId", obj));
	}
}
