/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.db.json.JsonSerializer;
import pl.edu.icm.unity.db.json.SerializersRegistry;
import pl.edu.icm.unity.db.mapper.GroupsMapper;
import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.DBLimits;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.model.GroupElementBean;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.EntityParam;
import pl.edu.icm.unity.types.Group;
import pl.edu.icm.unity.types.GroupContents;


/**
 * Groups related DB operations.
 * @author K. Benedyczak
 */
@Component
public class DBGroups
{
	private GroupResolver groupResolver;
	private IdentitiesResolver idResolver;
	private DBLimits limits;
	private JsonSerializer<Group> jsonS;
	
	@Autowired
	public DBGroups(GroupResolver groupResolver, IdentitiesResolver idResolver, SerializersRegistry reg, DB db)
	{
		this.groupResolver = groupResolver;
		this.idResolver = idResolver;
		this.limits = db.getDBLimits();
		jsonS = reg.getSerializer(Group.class);
	}
	
	/**
	 * Adds a new group. Pass null parent to create top-level ROOT group.
	 * @param parent
	 * @param name
	 * @throws InternalException
	 * @throws GroupNotKnownException
	 * @throws ElementAlreadyExistsException
	 */
	public void addGroup(Group toAdd, SqlSession sqlMap) 
		throws InternalException, IllegalGroupValueException
	{
		if (toAdd.getName().length() > limits.getNameLimit())
			throw new IllegalGroupValueException("Group name length must not exceed " + 
					limits.getNameLimit() + " characters");
			
		GroupsMapper mapper = sqlMap.getMapper(GroupsMapper.class);
		GroupBean pb = groupResolver.resolveGroup(toAdd.getParentPath(), mapper);

		GroupBean param = new GroupBean(pb.getId(), toAdd.getName());
		if (mapper.resolveGroup(param) != null)
			throw new IllegalGroupValueException("Group already exists");
		
		param.setContents(jsonS.toJson(toAdd));
		if (param.getContents().length > limits.getContentsLimit())
			throw new IllegalGroupValueException("Group metadata size (description, rules, ...) is too big.");
		mapper.insertGroup(param);
		sqlMap.clearCache();
	}
	
	public void removeGroup(String path, boolean recursive, SqlSession sqlMap) 
			throws InternalException, IllegalGroupValueException
	{
		GroupsMapper mapper = sqlMap.getMapper(GroupsMapper.class);
		GroupBean gb = groupResolver.resolveGroup(path, mapper);
		if (gb.getParent() == null)
			throw new IllegalGroupValueException("Can't remove the root group");
		if (!recursive)
		{
			if (mapper.getSubgroups(gb.getId()).size() > 0)
				throw new IllegalGroupValueException("The group contains subgroups");
		}
		mapper.deleteGroup(gb.getId());
	}
	
	
	public GroupContents getContents(String path, int filter, SqlSession sqlMap) 
			throws InternalException, IllegalGroupValueException
	{
		GroupsMapper mapper = sqlMap.getMapper(GroupsMapper.class);
		GroupBean gb = groupResolver.resolveGroup(path, mapper);

		GroupContents ret = new GroupContents();
		try
		{
			if ((filter & GroupContents.GROUPS) != 0)
			{
				List<GroupBean> subGroupsRaw = mapper.getSubgroups(gb.getId());
				ret.setSubGroups(convertGroups(subGroupsRaw, mapper));
			}
			if ((filter & GroupContents.LINKED_GROUPS) != 0)
			{
				List<GroupBean> linkedGroupsRaw = mapper.getLinkedGroups(gb.getId());
				ret.setLinkedGroups(convertGroups(linkedGroupsRaw, mapper));
			}
			if ((filter & GroupContents.MEMBERS) != 0)
			{
				List<BaseBean> membersRaw = mapper.getMembers(gb.getId());
				ret.setMembers(convertEntities(membersRaw));
			}
			if ((filter & GroupContents.METADATA) != 0)
			{
				//TODO retrieval of metadata
			}
		} catch (PersistenceException e)
		{
			throw new InternalException("Can't retrieve contents of the " + path + " group", e);
		}
		return ret;
	}
	
	public void addMemberFromParent(String path, EntityParam entity, SqlSession sqlMap)
	{
		GroupsMapper mapper = sqlMap.getMapper(GroupsMapper.class);
		GroupBean gb = groupResolver.resolveGroup(path, mapper);
		long entityId = idResolver.getEntityId(entity, sqlMap);
		if (gb.getParent() != null)
		{
			GroupElementBean param = new GroupElementBean(gb.getParent(), entityId);
			if (mapper.isMember(param) == null)
				throw new IllegalGroupValueException("Can't add to the group, as the entity is not a member of its parent group");
		}

		GroupElementBean param = new GroupElementBean(gb.getId(), entityId);
		if (mapper.isMember(param) != null)
			throw new IllegalGroupValueException("The entity is already a member a member of this group");
		mapper.insertMember(param);
	}
	
	public void removeMember(String path, EntityParam entity, SqlSession sqlMap)
	{
		GroupsMapper mapper = sqlMap.getMapper(GroupsMapper.class);
		GroupBean gb = groupResolver.resolveGroup(path, mapper);
		if (gb.getParent() == null)
			throw new IllegalGroupValueException("The entity can not be removed from the root group");
		long entityId = idResolver.getEntityId(entity, sqlMap);
		GroupElementBean param = new GroupElementBean(gb.getId(), entityId);
		if (mapper.isMember(param) == null)
			throw new IllegalGroupValueException("The entity is not a member of the group");
		
		mapper.deleteMember(param);
	}
	
	
	private List<Group> convertGroups(List<GroupBean> src, GroupsMapper mapper)
	{
		List<Group> ret = new ArrayList<Group>(src.size());
		for (int i=0; i<src.size(); i++)
			ret.add(groupResolver.resolveGroupBean(src.get(i), mapper));
		return ret;
	}
	
	private List<String> convertEntities(List<BaseBean> src)
	{
		List<String> ret = new ArrayList<String>(src.size());
		for (int i=0; i<src.size(); i++)
			ret.add(src.get(i).getId()+"");
		return ret;
	}

}
