/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.delegatedgroup;

import java.util.List;

import pl.edu.icm.unity.types.basic.Group;

/**
 * Holds information about delegated group contents.
 * @author P.Piernik
 *
 */
public class DelegatedGroupContents
{
	public final Group group;
	public final List<String> subGroups;
	public DelegatedGroupContents(Group group, List<String> subGroups)
	{
		this.group = group;
		this.subGroups = subGroups;
	}
}
