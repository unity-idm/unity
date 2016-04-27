/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.hz.GenericNamedHzCRUD;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.types.basic.Group;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;


/**
 * Hazelcast impl of {@link GroupDAO}
 * @author K. Benedyczak
 */
@Repository(GroupHzStore.STORE_ID)
public class GroupHzStore extends GenericNamedHzCRUD<Group> implements GroupDAO
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Autowired
	public GroupHzStore(GroupRDBMSStore rdbmsStore, HazelcastInstance hzInstance)
	{
		super(STORE_ID, NAME, GroupRDBMSStore.BEAN, rdbmsStore, hzInstance);
	}
	
	@Override
	public void updateByKey(long id, Group obj)
	{
		StorageLimits.checkNameLimit(obj.getName());
		TransactionalMap<Long, Group> hMap = getMap();
		Group old = hMap.get(id);
		if (old == null)
			throw new IllegalArgumentException(name + " [" + id + "] does not exists");
		if (!old.getName().equals(obj.getName()))
		{
			if (!old.getParentPath().equals(obj.getParentPath()))
				throw new IllegalArgumentException("It is not allowed to change group path, "
						+ "only rename is possible for " + old.getName() + 
						" (trying to rename to " + obj.getName() + ")");
			TransactionalMap<String, Long> nameMap = getNameMap();
			nameMap.remove(old.getName());
			nameMap.put(obj.getName(), id);
			updateChilderenPaths(old.getName(), obj.getName(), hMap, nameMap);
		}
		hMap.put(id, obj);
		HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(rdbmsCounterpartDaoName, "update", obj));
	}
	

	private void updateChilderenPaths(String oldPath, String newPath, TransactionalMap<Long, Group> hMap,
			TransactionalMap<String, Long> nameMap)
	{
		int oldPathLen = oldPath.length();
		for (long key: hMap.keySet())
		{
			Group gb = hMap.get(key);
			if (gb.getName().startsWith(oldPath) && !gb.getName().equals(oldPath))
			{
				String updatedPath = newPath + gb.getName().substring(oldPathLen);
				nameMap.remove(gb.getName());
				nameMap.put(updatedPath, key);

				gb.setPath(updatedPath);
				super.updateByKey(key, gb);
			}
		}
	}
}
