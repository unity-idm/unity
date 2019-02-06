/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.registration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupSelection;

/**
 * Breaks group membership change request into three groups: unchanged groups,
 * added groups and removed groups
 * 
 * @author P.Piernik
 *
 */
public class GroupDiffUtils
{
	public static RequestedGroupDiff getSingleGroupDiff(List<Group> allGroups, List<Group> allUserGroups, GroupSelection selected,
			GroupRegistrationParam formGroup)
	{

		List<Group> usersGroup = GroupPatternMatcher.filterByIncludeGroupsMode(
				GroupPatternMatcher.filterMatching(allUserGroups, formGroup.getGroupPath()),
				formGroup.getIncludeGroupsMode());

		List<String> selectedGroups = GroupPatternMatcher
				.filterByIncludeGroupsMode(
						GroupPatternMatcher.filterMatching(allGroups,
								selected.getSelectedGroups()),
						formGroup.getIncludeGroupsMode())
				.stream().map(g -> g.toString()).collect(Collectors.toList());

		Set<String> toAdd = new HashSet<>();
		Set<String> toRemove = new HashSet<>();
		Set<String> remain = new HashSet<>();

		remain.addAll(usersGroup.stream().map(g -> g.toString()).collect(Collectors.toSet()));
		remain.retainAll(selectedGroups);
		remain.addAll(usersGroup.stream().filter(g -> g.isTopLevel()).map(g -> g.toString())
				.collect(Collectors.toSet()));

		toRemove.addAll(usersGroup.stream().filter(g -> !g.isTopLevel()).map(g -> g.toString())
				.collect(Collectors.toSet()));
		toRemove.removeAll(remain);

		toAdd.addAll(selectedGroups);
		toAdd.removeAll(remain);

		return new RequestedGroupDiff(toAdd, filterGroupsForAddFromGroupsToRemove(toAdd, toRemove), remain);
	}

	public static RequestedGroupDiff getAllRequestedGroupsDiff(List<Group> allGroups, List<Group> allUserGroup,
			List<GroupSelection> groupSelections, List<GroupRegistrationParam> formGroupParams)
	{

		List<RequestedGroupDiff> diffs = new ArrayList<>();

		for (int i = 0; i < formGroupParams.size(); i++)
		{
			if (groupSelections.get(i) != null)
			{
				diffs.add(getSingleGroupDiff(allGroups, allUserGroup, groupSelections.get(i),
						formGroupParams.get(i)));
			}
		}

		Set<String> allAdd = new HashSet<>();
		Set<String> allRemain = new HashSet<>();
		Set<String> allRemove = new HashSet<>();

		for (RequestedGroupDiff diff : diffs)
		{
			allAdd.addAll(diff.toAdd);
			allRemain.addAll(diff.remain);
			allRemove.addAll(diff.toRemove);
		}

		allAdd.removeAll(allRemain);
		allRemove.removeAll(allRemain);

		return new RequestedGroupDiff(allAdd, filterGroupsForAddFromGroupsToRemove(allAdd, allRemove),
				allRemain);
	}

	private static Set<String> filterGroupsForAddFromGroupsToRemove(Set<String> toAdd, Set<String> toRemove)
	{
		Set<String> toRemoveFiltered = new HashSet<>();
		for (String rgroup : toRemove)
		{
			if (!isParent(rgroup, toAdd))
			{
				toRemoveFiltered.add(rgroup);
			}
		}
		return toRemoveFiltered;
	}

	private static boolean isParent(String group, Set<String> potentialChild)
	{
		for (String cgroup : potentialChild)
		{
			if (Group.isChildOrSame(cgroup, group))
			{
				return true;
			}
		}
		return false;
	}
}
