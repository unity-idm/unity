/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.model;

import pl.edu.icm.unity.types.I18nString;

import java.util.Objects;

public class Group
{
	public final String path;
	public final I18nString displayedName;
	public final String currentDisplayedName;
	public final boolean delegationEnabled;
	public final boolean subprojectsDelegationEnabled;
	public final String logoUrl;
	public final boolean isPublic;
	public final int level;

	public Group(String path, I18nString displayedName, String currentDisplayedName, boolean delegationEnabled, boolean subprojectsDelegationEnabled, String logoUrl, boolean isPublic, int level)
	{
		this.path = path;
		this.displayedName = displayedName;
		this.currentDisplayedName = currentDisplayedName;
		this.delegationEnabled = delegationEnabled;
		this.subprojectsDelegationEnabled = subprojectsDelegationEnabled;
		this.logoUrl = logoUrl;
		this.isPublic = isPublic;
		this.level = level;
	}

	public Group(Group group, int level)
	{
		this.path = group.path;
		this.displayedName = group.displayedName;
		this.currentDisplayedName = group.currentDisplayedName;
		this.delegationEnabled = group.delegationEnabled;
		this.subprojectsDelegationEnabled = group.subprojectsDelegationEnabled;
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
		return delegationEnabled == group.delegationEnabled &&
				subprojectsDelegationEnabled == group.subprojectsDelegationEnabled &&
				isPublic == group.isPublic &&
				level == group.level && Objects.equals(path, group.path) &&
				Objects.equals(displayedName, group.displayedName) &&
				Objects.equals(currentDisplayedName, group.currentDisplayedName) &&
				Objects.equals(logoUrl, group.logoUrl);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(path, displayedName, currentDisplayedName, delegationEnabled, subprojectsDelegationEnabled, logoUrl, isPublic, level);
	}

	@Override
	public String toString()
	{
		return "Group{" +
				"path='" + path + '\'' +
				", displayedName=" + displayedName +
				", currentDisplayedName='" + currentDisplayedName + '\'' +
				", delegationEnabled=" + delegationEnabled +
				", subprojectsDelegationEnabled=" + subprojectsDelegationEnabled +
				", logoUrl='" + logoUrl + '\'' +
				", isPublic=" + isPublic +
				", level=" + level +
				'}';
	}
}
