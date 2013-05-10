/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.resolvers.GroupResolver;

/**
 * DB operations which are used by multiple DB* classes
 * @author K. Benedyczak
 */
@Component
public class DBShared
{
	private GroupResolver groupResolver;

	@Autowired
	public DBShared(GroupResolver groupResolver)
	{
		this.groupResolver = groupResolver;
	}

	public Set<String> getAllGroups(long entityId, GroupsMapper gMapper)
	{
		List<GroupBean> groups = gMapper.getGroups4Entity(entityId);
		Set<String> ret = new HashSet<String>();
		for (GroupBean group: groups)
			ret.add(groupResolver.resolveGroupPath(group, gMapper));
		return ret;
	}
}
