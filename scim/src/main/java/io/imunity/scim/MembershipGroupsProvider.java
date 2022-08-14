/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.SCIMEndpointConfiguration;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.types.basic.Group;

@Component
class MembershipGroupsProvider
{
	private final GroupsManagement groupMan;

	MembershipGroupsProvider(@Qualifier("insecure") GroupsManagement groupMan)
	{
		this.groupMan = groupMan;
	}

	List<String> getEffectiveMembershipGroups(SCIMEndpointConfiguration scimEndpointConfiguration)
	{
		Map<String, Group> allGroups;
		try
		{
			allGroups = groupMan.getAllGroups();
		} catch (EngineException e)
		{
			throw new RuntimeEngineException("Can not get groups", e);
		}

		List<Group> allGroupsList = allGroups.values().stream().collect(Collectors.toList());

		Set<Group> whiteListGroups = scimEndpointConfiguration.membershipGroups.stream()
				.map(g -> GroupPatternMatcher.filterMatching(allGroupsList, g)).flatMap(a -> a.stream())
				.collect(Collectors.toSet());
		addChildrenGroups(whiteListGroups, allGroupsList);
		Set<Group> blackListGroups = scimEndpointConfiguration.excludedMembershipGroups.stream()
				.map(g -> GroupPatternMatcher.filterMatching(allGroupsList, g)).flatMap(a -> a.stream())
				.collect(Collectors.toSet());

		whiteListGroups.removeAll(blackListGroups);
		return whiteListGroups.stream().map(g -> g.getPathEncoded()).sorted().collect(Collectors.toList());
	}

	private void addChildrenGroups(Set<Group> whiteListGroups, List<Group> allGroupsList)
	{
		for (Group g : whiteListGroups)
		{
			allGroupsList.forEach(ag ->
			{
				if (Group.isChild(ag.getPathEncoded(), g.getPathEncoded()))
				{
					whiteListGroups.add(ag);
				}
			});
		}
	}

}
