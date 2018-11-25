/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.types.delegatedgroup;

import java.util.List;

/**
 * Holds information about delegated group and subgroup.
 * @author P.Piernik
 *
 */
public class DelegatedGroupContents
{
	public final DelegatedGroup group;
	public final List<String> subGroups;
	public DelegatedGroupContents(DelegatedGroup group, List<String> subGroups)
	{
		this.group = group;
		this.subGroups = subGroups;
	}
}
