/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.bulk;

import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.GroupContents;

/**
 * Supports fast resolving of data about a group contents in bulk. Usage pattern:
 * first call {@link #getBulkMembershipData(String)} to obtain a data object. This is the slowest part.
 * Then use it as an argument to other, fast methods converting it to desired contents.
 *  
 * @author K. Benedyczak
 */
public interface BulkGroupQueryService
{
	
	GroupMembershipData getBulkMembershipData(String group) throws EngineException;
	
	GroupMembershipData getBulkMembershipData(String group, Set<Long> filter) throws EngineException;

	GroupStructuralData getBulkStructuralData(String group) throws EngineException;
	
	Map<Long, Map<String, AttributeExt>> getGroupUsersAttributes(String group, GroupMembershipData dataO);

	Map<Long, Entity> getGroupEntitiesNoContextWithTargeted(GroupMembershipData dataO);

	Map<Long, Entity> getGroupEntitiesNoContextWithoutTargeted(GroupMembershipData dataO);
	
	/**
	 * @return keys of the returned map include the selected group and all its children. Values are 
	 * objects with group's metadata and subgroups (but without members)
	 */
	Map<String, GroupContents> getGroupAndSubgroups(GroupStructuralData dataO);
	
	Map<Long, GroupMembershipInfo> getMembershipInfo(GroupMembershipData dataO);

	
}
