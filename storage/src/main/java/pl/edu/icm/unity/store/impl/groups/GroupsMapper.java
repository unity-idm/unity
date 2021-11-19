/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.groups;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import pl.edu.icm.unity.store.rdbms.NamedCRUDMapper;


/**
 * Access to the Groups.xml operations.
 * @author K. Benedyczak
 */
public interface GroupsMapper extends NamedCRUDMapper<GroupBean>
{
	List<GroupBean> getSubgroups(String parentPath);
	long createRoot(GroupBean obj);
	List<GroupBean> getByNames(@Param("groupList") List<String> groups);
}
