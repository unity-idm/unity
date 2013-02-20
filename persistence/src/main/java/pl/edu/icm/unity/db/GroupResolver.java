/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.JsonSerializer;
import pl.edu.icm.unity.db.json.SerializersRegistry;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.Group;

/**
 * Serializes Group to GroupBean and vice-versa
 * @author K. Benedyczak
 */
@Component
public class GroupResolver
{
	public static final String ROOT_GROUP_NAME = "ROOT";
	private JsonSerializer<Group> jsonS;
	
	@Autowired
	public GroupResolver(SerializersRegistry reg)
	{
		jsonS = reg.getSerializer(Group.class);
	}
	
	/**
	 * Low level resolve of a group: finds it in a given parent group by name.
	 * Note - this method does not perform loading of group JSON contents.
	 * @param name
	 * @param parentId can be null
	 * @param mapper
	 * @return 
	 * @throws InternalException
	 * @throws GroupNotKnownException
	 */
	private GroupBean resolveGroup(String name, Integer parentId, GroupsMapper mapper) 
			throws InternalException, IllegalGroupValueException
	{
		GroupBean res = null;
		GroupBean param = new GroupBean(parentId, name);
		try
		{
			res = mapper.resolveGroup(param);
		} catch (PersistenceException e)
		{
			String msg = "DB error: Can't resolve group " + name + 
					" with parent " + parentId;
			throw new InternalException(msg, e);  
		}
		if (res == null)
			throw new IllegalGroupValueException("Group " + name + " is not known");
		return res;
	}

	/**
	 * High level, recursive group resolve.
	 * @param g
	 * @param mapper
	 * @return
	 * @throws InternalException
	 * @throws GroupNotKnownException
	 */
	public GroupBean resolveGroup(String groupPath, GroupsMapper mapper) 
			throws InternalException, IllegalGroupValueException
	{
		Group group = new Group(groupPath);
		String path[] = group.getPath();
		GroupBean b = resolveGroup(ROOT_GROUP_NAME, null, mapper);
		Integer p = b.getId();
		for (int i=0; i<path.length; i++)
		{
			b = resolveGroup(path[i], p, mapper);
			p = b.getId();
		}
		return b;
	}

	/**
	 * Converts {@link GroupBean} into a {@link Group}
	 * @param gb
	 * @param mapper
	 * @return
	 */
	public Group resolveGroupBean(GroupBean gb, GroupsMapper mapper)
	{
		StringBuilder path = new StringBuilder();
		
		GroupBean parent = gb;
		while (parent.getParent() != null)
		{
			path.insert(0, '/' + parent.getName());
			parent = mapper.getGroup(parent.getParent());
		}
		Group group = new Group(path.toString());
		jsonS.fromJson(gb.getContents(), group);
		return group;
	}

}
