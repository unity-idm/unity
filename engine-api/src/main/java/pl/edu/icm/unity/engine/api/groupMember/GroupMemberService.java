/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.groupMember;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.SimpleGroupMember;

import java.util.List;
import java.util.Map;

public interface GroupMemberService
{
	Group getGroup(String group) throws EngineException;

	List<SimpleGroupMember> getGroupMembers(String group, List<String> attributes) throws EngineException;

	Map<String, List<SimpleGroupMember>> getGroupMembers(List<String> groups, List<String> attributes) throws EngineException;
}
