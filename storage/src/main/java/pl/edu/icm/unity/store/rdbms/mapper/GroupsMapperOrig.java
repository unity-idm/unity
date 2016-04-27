/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.rdbms.mapper;

import java.util.List;

import pl.edu.icm.unity.store.rdbms.model.GroupBean;
import pl.edu.icm.unity.store.rdbms.model.GroupElementBean;
import pl.edu.icm.unity.store.rdbms.model.GroupElementChangeBean;


/**
 * Access to the Groups.xml operations.
 * @author K. Benedyczak
 */
public interface GroupsMapperOrig
{
	/**
	 * param must have the name and parent ID set
	 * @param gb
	 * @return
	 */
	GroupBean resolveGroup(GroupBean gb);
	
	
	void insertGroup(GroupBean group);
	void updateGroup(GroupBean group);
	void deleteGroup(long id);
	
	List<GroupBean> getSubgroups(long parentId);
	List<GroupBean> getGroups4Entity(long entityId);
	List<GroupElementBean> getGroupMembership4Entity(long entityId);
	List<GroupBean> getLinkedGroups(long parentId);
	List<GroupElementBean> getMembers(long groupId);
	
	GroupBean getGroup(long id);
	List<GroupBean> getAllGroups();
	
	GroupElementBean isMember(GroupElementBean param);
	void insertMember(GroupElementBean param);
	void deleteMember(GroupElementBean param);
	
	void updateMeembership(GroupElementChangeBean param);
}
