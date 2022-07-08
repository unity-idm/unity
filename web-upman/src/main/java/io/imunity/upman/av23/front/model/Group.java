/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.model;

import java.util.Objects;

public class Group
{
	public final String path;
	public final String displayedName;
	public final boolean delegationEnabled;
	public final int level;

	public Group(String path, String displayedName, boolean delegationEnabled, int level)
	{
		this.path = path;
		this.displayedName = displayedName;
		this.delegationEnabled = delegationEnabled;
		this.level = level;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Group group = (Group) o;
		return delegationEnabled == group.delegationEnabled && level == group.level && Objects.equals(path, group.path) && Objects.equals(displayedName, group.displayedName);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(path, displayedName, delegationEnabled, level);
	}

	@Override
	public String toString()
	{
		return "Group{" +
				"path='" + path + '\'' +
				", displayedName='" + displayedName + '\'' +
				", delegationEnabled=" + delegationEnabled +
				", level=" + level +
				'}';
	}
}
