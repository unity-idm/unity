/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.mapper;

import java.util.List;

import pl.edu.icm.unity.db.model.GroupBean;


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
	public void deleteGroup(int id);
	
	public List<GroupBean> getSubgroups(int parentId);
	public List<GroupBean> getLinkedGroups(int parentId);
	
	public GroupBean getGroup(int id);
}
