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
 * Creates group diff based on actual users group, group selected by user and
 * form registration params.
 * 
 * @author P.Piernik
 *
 */
public class GroupDiffUtils
{
	public static RequestedGroupDiff getGroupDiff(List<Group> allUserGroups, GroupSelection selected,
			GroupRegistrationParam formGroup)
	{

		List<String> usersGroup = GroupPatternMatcher.filterMatching(allUserGroups, formGroup.getGroupPath())
				.stream().filter(g -> !g.isTopLevel()).map(g -> g.toString())
				.collect(Collectors.toList());

		List<String> selectedGroups = selected.getSelectedGroups();
		Set<String> toAdd = new HashSet<>();
		Set<String> toRemove = new HashSet<>();
		Set<String> remain = new HashSet<>();
		remain.add("/");

		for (String group : usersGroup)
		{
			if (selectedGroups.contains(group))
			{
				remain.add(group);
			} else
			{
				toRemove.add(group);
			}
		}

		for (String groupSelected : selectedGroups)
		{
			if (!usersGroup.contains(groupSelected))
			{
				toAdd.add(groupSelected);
			}
		}
		return new RequestedGroupDiff(toAdd, toRemove, remain);
	}

	public static RequestedGroupDiff getGlobalDiff(List<Group> allUserGroup, List<GroupSelection> groupSelections,
			List<GroupRegistrationParam> formGroupParams)
	{

		List<RequestedGroupDiff> diffs = new ArrayList<>();

		for (int i = 0; i < formGroupParams.size(); i++)
		{
			if (groupSelections.get(i) != null)
			{
				diffs.add(getGroupDiff(allUserGroup, groupSelections.get(i), formGroupParams.get(i)));
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

		for (String remainGroup : allRemain)
		{
			if (allAdd.contains(remainGroup))
			{
				allAdd.remove(remainGroup);

			}

			if (allRemove.contains(remainGroup))
			{
				allRemove.remove(remainGroup);
			}
		}

		for (String groupToAdd : allAdd)
		{
			if (allRemove.contains(groupToAdd))
			{
				allRemove.remove(groupToAdd);
			}
		}

		return new RequestedGroupDiff(allAdd, allRemove, allRemain);
	}
}
