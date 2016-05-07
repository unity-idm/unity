/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.hz.GenericNamedHzCRUD;
import pl.edu.icm.unity.types.basic2.Group;

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
	public GroupHzStore(GroupRDBMSStore rdbmsStore)
	{
		super(STORE_ID, NAME, GroupRDBMSStore.BEAN, rdbmsStore);
		addRemovalHandler(this::cascadeParentRemoval);
		addUpdateHandler(this::cascadeParentRename);
	}
	
	@Override
	protected void preUpdateCheck(Group old, Group updated)
	{
		if (!old.getParentPath().equals(updated.getParentPath()))
			throw new IllegalArgumentException("It is not allowed to change group path, "
					+ "only rename is possible for " + old.getName() + 
					" (trying to rename to " + updated.getName() + ")");
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
				super.updateByKey(key, gb);
			}
		}
	}
}
