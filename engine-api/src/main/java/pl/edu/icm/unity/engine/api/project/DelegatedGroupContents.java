/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Holds information about delegated group and subgroup.
 * 
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
		this.subGroups = new ArrayList<>();
		if (subGroups != null)
		{
			this.subGroups.addAll(subGroups);
		}

	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.group, this.subGroups);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final DelegatedGroupContents other = (DelegatedGroupContents) obj;
		return Objects.equals(this.group, other.group) && Objects.equals(this.subGroups, other.subGroups);

	}
}
