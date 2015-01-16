/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.GroupsSerializer;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.GroupResolver;
import pl.edu.icm.unity.types.basic.Group;

/**
 * DB operations which are used by multiple DB* classes
 * @author K. Benedyczak
 */
@Component
public class DBShared
{
	private GroupResolver groupResolver;
	private GroupsSerializer jsonS;

	@Autowired
	public DBShared(GroupResolver groupResolver, GroupsSerializer groupsSerializer)
	{
		this.groupResolver = groupResolver;
		this.jsonS = groupsSerializer;
	}

	public Set<String> getAllGroups(long entityId, SqlSession sqlMap)
	{
		GroupsMapper gMapper = sqlMap.getMapper(GroupsMapper.class);
		return getAllGroups(entityId, gMapper);
	}
	
	public Set<String> getAllGroups(long entityId, GroupsMapper gMapper)
	{
		List<GroupBean> groups = gMapper.getGroups4Entity(entityId);
		Set<String> ret = new HashSet<String>();
		for (GroupBean group: groups)
			ret.add(groupResolver.resolveGroupPath(group, gMapper));
		return ret;
	}
	
	public Set<Group> getAllGroupsWithNames(long entityId, SqlSession sqlMap)
	{
		GroupsMapper gMapper = sqlMap.getMapper(GroupsMapper.class);
		List<GroupBean> groups = gMapper.getGroups4Entity(entityId);
		Set<Group> ret = new HashSet<Group>();
		for (GroupBean group: groups)
		{
			Group resolved = new Group(groupResolver.resolveGroupPath(group, gMapper));
			jsonS.fillFromJsonMinimal(group.getContents(), resolved);
			ret.add(resolved);
		}
		return ret;
	}
}
