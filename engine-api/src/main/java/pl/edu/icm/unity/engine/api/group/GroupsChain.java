/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.group;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.group.Group;

public class GroupsChain
{
	public final List<Group> groups;

	public GroupsChain(List<Group> groups)
	{
		this.groups = Collections.unmodifiableList(groups.stream().sorted().collect(Collectors.toList()));
	}

	public Group getRoot()
	{
		return groups.get(0);
	}

	public Group getLast()
	{
		return groups.get(groups.size() - 1);
	}

	public GroupsChain getParentChain()
	{
		if (groups.size() == 1)
		{
			return null;
		}

		return new GroupsChain(groups.subList(0, groups.size() - 1));
	}
}
