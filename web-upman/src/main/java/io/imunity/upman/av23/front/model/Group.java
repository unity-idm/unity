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
	public final boolean delegationEnableSubprojects;
	public final String logoUrl;
	public final boolean isPublic;
	public final int level;

	public Group(String path, String displayedName, boolean delegationEnabled, boolean delegationEnableSubprojects, String logoUrl, boolean isPublic, int level)
	{
		this.path = path;
		this.displayedName = displayedName;
		this.delegationEnabled = delegationEnabled;
		this.delegationEnableSubprojects = delegationEnableSubprojects;
		this.logoUrl = logoUrl;
		this.isPublic = isPublic;
		this.level = level;
	}

	public Group(Group group, int level)
	{
		this.path = group.path;
		this.displayedName = group.displayedName;
		this.delegationEnabled = group.delegationEnabled;
		this.delegationEnableSubprojects = group.delegationEnableSubprojects;
		this.logoUrl = group.logoUrl;
		this.isPublic = group.isPublic;
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
