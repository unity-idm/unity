/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;

import static java.util.stream.Collectors.toList;


/**
 * Group membership DAO
 * @author K. Benedyczak
 */
public interface MembershipDAO
{
	String DAO_ID = "MembershipDAO";
	String NAME = "group membership";
	
	void create(GroupMembership obj);
	
	void deleteByKey(long entityId, String group);

	boolean isMember(long entityId, String group);

	List<GroupMembership> getEntityMembership(long entityId);

	List<GroupMembership> getMembers(String group);
	
	List<GroupMembership> getAll();
	
	default Set<String> getEntityMembershipSimple(long entityId)
	{
		List<GroupMembership> full = getEntityMembership(entityId);
		Set<String> ret = new HashSet<>(full.size());
		for (GroupMembership gm: full)
			ret.add(gm.getGroup());
		return ret;
	}

	default List<Group> getEntityMembershipGroups(long entityId)
	{
		return getEntityMembership(entityId).stream()
			.map(GroupMembership::getGroup)
			.map(Group::new)
			.collect(toList());
	}
}
