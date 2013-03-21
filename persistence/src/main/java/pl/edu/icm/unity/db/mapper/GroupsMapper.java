/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.mapper;

import java.util.List;

import pl.edu.icm.unity.db.model.BaseBean;
import pl.edu.icm.unity.db.model.GroupBean;
import pl.edu.icm.unity.db.model.GroupElementBean;


/**
 * Access to the Groups.xml operations.
 * @author K. Benedyczak
 */
public interface GroupsMapper
{
	/**
	 * param must have the name and parent ID set
	 * @param gb
	 * @return
	 */
	public GroupBean resolveGroup(GroupBean gb);
	
	
	public void insertGroup(GroupBean group);
	public void updateGroup(GroupBean group);
	public void deleteGroup(long id);
	
	public List<GroupBean> getSubgroups(long parentId);
	public List<GroupBean> getGroups4Entity(long entityId);
	public List<GroupBean> getLinkedGroups(long parentId);
	public List<BaseBean> getMembers(long groupId);
	
	public GroupBean getGroup(long id);
	public List<GroupBean> getAllGroups();
	
	public GroupElementBean isMember(GroupElementBean param);
	public void insertMember(GroupElementBean param);
	public void deleteMember(GroupElementBean param);
}
