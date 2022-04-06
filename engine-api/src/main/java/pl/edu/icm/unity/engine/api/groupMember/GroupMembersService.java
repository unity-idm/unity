/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.groupMember;

import java.util.List;
import java.util.Map;

public interface GroupMembersService
{
	/**
	 * Method returns list of group members with attributes in selected group.
	 * Argument attributes decides about what attributes will be return.
	 * If null or empty is returns all group members with all attributes in selected group.
	 */
	List<GroupMemberWithAttributes> getGroupsMembersWithSelectedAttributes(String group, List<String> attributes);

	/**
	 * Method returns list of group members with attributes in selected groups.
	 * Argument attributes decides about what attributes will be return.
	 * If null or empty it returns all group members with all attributes in selected groups.
	 */
	Map<String, List<GroupMemberWithAttributes>> getGroupsMembersInGroupsWithSelectedAttributes(List<String> groups, List<String> attributes);
}
