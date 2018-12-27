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

		List<Group> usersGroup = GroupPatternMatcher.filterMatching(allUserGroups, formGroup.getGroupPath());
		List<String> usersGroupWithoutRoot = usersGroup.stream().filter(g -> !g.isTopLevel()).map(g -> g.toString())
				.collect(Collectors.toList());
		
		List<String> selectedGroups = selected.getSelectedGroups();
		Set<String> toAdd = new HashSet<>();
		Set<String> toRemove = new HashSet<>();
		Set<String> remain = usersGroup.stream().filter(g -> g.isTopLevel()).map(g -> g.toString())
				.collect(Collectors.toSet());

		for (String group : usersGroupWithoutRoot)
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
			if (!usersGroupWithoutRoot.contains(groupSelected) && !remain.contains(groupSelected))
			{
				toAdd.add(groupSelected);
			}
		}

		Set<String> toRemoveFiltered = new HashSet<>();
		for (String rgroup : toRemove)
		{
			if (!isParent(rgroup, toAdd))
			{
				toRemoveFiltered.add(rgroup);
			}
		}
		//should not happend, 
		if (toRemoveFiltered.contains("/"))
		{
			toRemoveFiltered.remove("/");
		}
		
		return new RequestedGroupDiff(toAdd, toRemoveFiltered, remain);
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

		Set<String> toRemoveFiltered = new HashSet<>();
		for (String rgroup : allRemove)
		{
			if (!isParent(rgroup, allAdd))
			{
				toRemoveFiltered.add(rgroup);
			}
		}

		return new RequestedGroupDiff(allAdd, toRemoveFiltered, allRemain);
	}
}
