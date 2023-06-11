/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;

public class MembershipGroupsUtils
{

	public static List<String> getEffectiveMembershipGroups(List<String> membershipGroups,
			List<String> excludedMembershipGroups, Map<String, Group> allGroups)
	{

		List<Group> allGroupsList = allGroups.values().stream().collect(Collectors.toList());

		Set<Group> whiteListGroups = membershipGroups.stream()
				.map(g -> GroupPatternMatcher.filterMatching(allGroupsList, g)).flatMap(a -> a.stream())
				.collect(Collectors.toSet());
		whiteListGroups = addChildrenGroups(whiteListGroups, allGroupsList);
		Set<Group> blackListGroups = excludedMembershipGroups.stream()
				.map(g -> GroupPatternMatcher.filterMatching(allGroupsList, g)).flatMap(a -> a.stream())
				.collect(Collectors.toSet());
		blackListGroups = addChildrenGroups(blackListGroups, allGroupsList);

		whiteListGroups.removeAll(blackListGroups);
		return whiteListGroups.stream().map(g -> g.getPathEncoded()).sorted().collect(Collectors.toList());
	}

	private static Set<Group> addChildrenGroups(Set<Group> groupsToResolve, List<Group> allGroupsList)
	{
		Set<Group> ret = new HashSet<>(groupsToResolve);

		for (Group g : groupsToResolve)
		{
			allGroupsList.forEach(ag ->
			{
				if (Group.isChild(ag.getPathEncoded(), g.getPathEncoded()))
				{
					ret.add(ag);
				}
			});
		}
		return ret;
	}
}
