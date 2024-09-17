/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.store.impl.StorageLimits;
import pl.edu.icm.unity.store.rdbms.GenericNamedRDBMSCRUD;
import pl.edu.icm.unity.store.rdbms.tx.SQLTransactionTL;


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
	public List<Long> createList(List<Group> objs)
	{
		Set<String> parentsUsed = new HashSet<>();
		for (Group grp: objs)
		{
			StorageLimits.checkNameLimit(grp.getName());
			parentsUsed.add(grp.getParentPath());
		}
		GroupsMapper mapper = SQLTransactionTL.getSql().getMapper(GroupsMapper.class);
		
		List<GroupBean> byNames = mapper.getByNames(new ArrayList<>(parentsUsed));
		Map<String, GroupBean> resolvedParents = byNames.stream()
				.collect(Collectors.toMap(grp -> grp.getName(), grp -> grp));
		
		List<GroupBean> converted = new ArrayList<>(objs.size());
		for (Group obj: objs)
		{
			GroupBean toAdd = jsonSerializer.toDB(obj);
			assertContentsLimit(toAdd.getContents());
			toAdd.setParentId((int)(long)(resolvedParents.get(obj.getParentPath()).getId()));
			converted.add(toAdd);
		}
		List<GroupBean> allBeforeAdd = mapper.getAll();
		mapper.createList(converted);
		return mapper.getAll().stream().filter(g -> !allBeforeAdd.contains(g)).map(g -> g.getId()).toList();
	}
	
	@Override
	public void updateByKey(long key, Group obj)
	{
		StorageLimits.checkNameLimit(obj.getName());
		GroupsMapper mapper = SQLTransactionTL.getSql().getMapper(GroupsMapper.class);
		GroupBean oldBean = mapper.getByKey(key);
		if (oldBean == null)
			throw new IllegalArgumentException(elementName + " with key [" + key + 
					"] does not exist");
		if (!oldBean.getName().equals(obj.getName()))
		{
			if (oldBean.getName().equals("/"))
				throw new IllegalArgumentException("It is not allowed to rename the root group");
			if (!oldBean.getParent().equals(obj.getParentPath()))
				throw new IllegalArgumentException("It is not allowed to change group path, "
						+ "only rename is possible for " + oldBean.getName() + 
						" (trying to rename to " + obj.getName() + ")");
			updateChilderenPaths(oldBean.getName(), obj.getName(), mapper);
		}
		Group old = jsonSerializer.fromDB(oldBean);
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

	@Override
	public List<Group> getGroupChain(String path)
	{
		GroupsMapper mapper = SQLTransactionTL.getSql().getMapper(GroupsMapper.class);
		List<Group> ret = new ArrayList<>();
		Group grp = new Group(path);
		for (GroupBean bean: mapper.getByNames(grp.getPathsChain()))
		{
			Group obj = jsonSerializer.fromDB(bean);
			ret.add(obj);
		}
		return ret;
	}
}
