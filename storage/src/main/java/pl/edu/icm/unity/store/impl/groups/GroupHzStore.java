/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hazelcast.core.TransactionalMap;

import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.hz.GenericNamedHzCRUD;
import pl.edu.icm.unity.store.hz.rdbmsflush.RDBMSMutationEvent;
import pl.edu.icm.unity.store.hz.tx.HzTransactionTL;
import pl.edu.icm.unity.types.basic.Group;


/**
 * Hazelcast impl of {@link GroupDAO}
 * @author K. Benedyczak
 */
@Repository(GroupHzStore.STORE_ID)
public class GroupHzStore extends GenericNamedHzCRUD<Group> implements GroupDAOInternal
{
	public static final String STORE_ID = DAO_ID + "hz";

	@Autowired
	public GroupHzStore(GroupRDBMSStore rdbmsStore)
	{
		super(STORE_ID, NAME, GroupRDBMSStore.BEAN, rdbmsStore);
		addRemovalHandler(this::cascadeParentRemoval);
		addUpdateHandler(this::cascadeParentRename);
	}
	
	@Override
	protected void preUpdateCheck(Group old, Group updated)
	{
		if (old.isTopLevel() != updated.isTopLevel() || 
				(!old.isTopLevel() && !old.getParentPath().equals(updated.getParentPath())))
			throw new IllegalArgumentException("It is not allowed to change group path, "
					+ "only rename is possible for " + old.getName() + 
					" (trying to rename to " + updated.getName() + ")");
	}
	
	@Override
	public void deleteAll()
	{
		TransactionalMap<Long, Group> hMap = getMap();
		Set<Long> keySet = hMap.keySet();
		for (Long key: keySet)
		{
			Group removed = super.deleteByKeyRet(key, false);
			getNameMap().remove(removed.getName());
			if (removed.isTopLevel())
				HzTransactionTL.enqueueRDBMSMutation(new RDBMSMutationEvent(
					rdbmsCounterpartDaoName, "deleteByKey", key));
		}
	}
	
	private void cascadeParentRemoval(long id, String name)
	{
		TransactionalMap<String, Long> nameMap = getNameMap();
		
		for (String group: nameMap.keySet())
		{
			if (Group.isChild(group, name))
				deleteByKey(nameMap.get(group), false);
		}
	}

	private void cascadeParentRename(long id, String oldPath, Group newObj)
	{
		TransactionalMap<Long, Group> hMap = getMap();
		TransactionalMap<String, Long> nameMap = getNameMap();
		String newPath = newObj.getName(); 
		int oldPathLen = oldPath.length();
		for (long key: hMap.keySet())
		{
			Group gb = hMap.get(key);
			if (Group.isChild(gb.getName(), oldPath))
			{
				String updatedPath = newPath + gb.getName().substring(oldPathLen);
				nameMap.remove(gb.getName());
				nameMap.put(updatedPath, key);

				gb.setPath(updatedPath);
				super.updateByKey(key, gb, false);
			}
		}
	}
}
