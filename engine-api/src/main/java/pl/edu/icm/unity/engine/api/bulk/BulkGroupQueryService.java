/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.bulk;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupContents;

import java.util.Map;
import java.util.Set;

/**
 * Supports fast resolving of data about a group contents in bulk. Usage pattern:
 * first call {@link #getBulkMembershipData(String)} to obtain a data object. This is the slowest part.
 * Then use it as an argument to other, fast methods converting it to desired contents.
 * 
 */
public interface BulkGroupQueryService
{
	//TODO this method should not be in this service. It is higher-level, it internally gets bulk data and then resolves it.
	GroupsWithMembers getMembersWithAttributeForAllGroups(String rootGroup, Set<String> groupFilter);
	
	GroupMembershipData getBulkMembershipData(String group) throws EngineException;

	GroupMembershipData getBulkMembershipData(String group, Set<Long> filter) throws EngineException;

	Map<Long, Map<String, AttributeExt>> getGroupUsersAttributes(String group, GroupMembershipData dataO);

	Map<Long, Entity> getGroupEntitiesNoContextWithTargeted(GroupMembershipData dataO);

	Map<Long, Entity> getGroupEntitiesNoContextWithoutTargeted(GroupMembershipData dataO);

	Map<Long, EntityInGroupData> getMembershipInfo(GroupMembershipData dataO);
	
	
	GroupStructuralData getBulkStructuralData(String group) throws EngineException;

	/**
	 * @return keys of the returned map include the selected group and all its children. Values are 
	 * objects with group's metadata and subgroups (but without members)
	 */
	Map<String, GroupContents> getGroupAndSubgroups(GroupStructuralData dataO);

	Map<String, GroupContents> getGroupAndSubgroups(GroupStructuralData dataO, String rootGroup);
}