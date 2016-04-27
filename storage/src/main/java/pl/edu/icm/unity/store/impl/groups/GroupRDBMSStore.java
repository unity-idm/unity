/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.mapper.GroupsMapper;
import pl.edu.icm.unity.store.rdbms.model.GroupBean;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.types.basic.Group;


/**
 * RDBMS storage of {@link Group}
 * @author K. Benedyczak
 */
@Repository(GroupRDBMSStore.BEAN)
public class GroupRDBMSStore extends GenericNamedRDBMSCRUD<Group, GroupBean> implements GroupDAO
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public GroupRDBMSStore(GroupJsonSerializer jsonSerializer, StorageLimits limits)
	{
		super(GroupsMapper.class, jsonSerializer, NAME, limits);
	}
	
	@Override
	public void updateByKey(long key, Group obj)
	{
		limits.checkNameLimit(obj.getName());
		GroupsMapper mapper = SQLTransactionTL.getSql().getMapper(GroupsMapper.class);
		GroupBean old = mapper.getByKey(key);
		if (old == null)
			throw new IllegalArgumentException(elementName + " with key [" + key + 
					"] does not exist");
		if (!old.getName().equals(obj.getName()))
		{
			if (!old.getParent().equals(obj.getParentPath()))
				throw new IllegalArgumentException("It is not allowed to change group path, "
						+ "only rename is possible for " + old.getName() + 
						" (trying to rename to " + obj.getName() + ")");
			updateChilderenPaths(old.getName(), obj.getName(), mapper);
		}
		GroupBean toUpdate = jsonSerializer.toDB(obj);
		limits.checkContentsLimit(toUpdate.getContents());
		toUpdate.setId(key);
		mapper.updateByKey(toUpdate);		
	}

	private void updateChilderenPaths(String oldPath, String newPath, GroupsMapper mapper)
	{
		List<GroupBean> all = mapper.getAll();
		int oldPathLen = oldPath.length();
		for (GroupBean gb: all)
		{
			if (gb.getName().startsWith(oldPath) && !gb.getName().equals(oldPath))
			{
				String updatedPath = newPath + gb.getName().substring(oldPathLen);
				String updatedPPath = newPath + gb.getParent().substring(oldPathLen);
				gb.setName(updatedPath);
				gb.setParent(updatedPPath);
				mapper.updateByKey(gb);
			}
		}
	}
}
