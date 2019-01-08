/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.sql.SQLException;
import java.util.List;

import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;
import pl.edu.icm.unity.types.basic.Group;


/**
 * RDBMS storage of {@link Group} with caching
 * @author K. Benedyczak
 */
@Repository(GroupRDBMSStore.BEAN)
public class GroupRDBMSStore extends GenericNamedRDBMSCRUD<Group, GroupBean> implements GroupDAOInternal
{
	public static final String BEAN = DAO_ID + "rdbms";

	@Autowired
	public GroupRDBMSStore(GroupJsonSerializer jsonSerializer)
	{
		super(GroupsMapper.class, jsonSerializer, NAME);
	}
	
	@Override
	public long create(Group obj)
	{
		StorageLimits.checkNameLimit(obj.getName());
		try
		{
			GroupsMapper mapper = SQLTransactionTL.getSql().getMapper(GroupsMapper.class);
			GroupBean toAdd = jsonSerializer.toDB(obj);
			StorageLimits.checkContentsLimit(toAdd.getContents());
			if (!obj.isTopLevel())
			{
				mapper.create(toAdd);
			} else
			{
				mapper.createRoot(toAdd);
			}
			return toAdd.getId();				
		} catch (PersistenceException e)
		{
			Throwable causeO = e.getCause();
			if (!(causeO instanceof SQLException))
				throw e;
			SQLException cause = (SQLException) causeO;
			if (cause.getSQLState().equals(SQL_DUP_1_ERROR) || 
					cause.getSQLState().equals(SQL_DUP_2_ERROR))
				throw new IllegalArgumentException(elementName + " [" + obj.getName() + 
						"] already exist", e);
			throw e;
		}
	}
	
	@Override
	public void updateByKey(long key, Group obj)
	{
		StorageLimits.checkNameLimit(obj.getName());
		GroupsMapper mapper = SQLTransactionTL.getSql().getMapper(GroupsMapper.class);
		GroupBean old = mapper.getByKey(key);
		if (old == null)
			throw new IllegalArgumentException(elementName + " with key [" + key + 
					"] does not exist");
		if (!old.getName().equals(obj.getName()))
		{
			if (old.getName().equals("/"))
				throw new IllegalArgumentException("It is not allowed to rename the root group");
			if (!old.getParent().equals(obj.getParentPath()))
				throw new IllegalArgumentException("It is not allowed to change group path, "
						+ "only rename is possible for " + old.getName() + 
						" (trying to rename to " + obj.getName() + ")");
			updateChilderenPaths(old.getName(), obj.getName(), mapper);
		}
		preUpdateCheck(old, obj);
		firePreUpdate(key, obj.getName(), obj, old);
		GroupBean toUpdate = jsonSerializer.toDB(obj);
		StorageLimits.checkContentsLimit(toUpdate.getContents());
		toUpdate.setId(key);
		mapper.updateByKey(toUpdate);		
	}

	private void updateChilderenPaths(String oldPath, String newPath, GroupsMapper mapper)
	{
		List<GroupBean> all = mapper.getAll();
		int oldPathLen = oldPath.length();
		for (GroupBean gb: all)
		{
			if (Group.isChild(gb.getName(), oldPath) && !gb.getName().equals(oldPath))
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
