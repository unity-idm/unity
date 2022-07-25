/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.model;

import java.util.Objects;

public class ProjectGroup
{
	public final String path;
	public final String displayedName;

	public ProjectGroup(String path, String displayedName)
	{
		this.path = path;
		this.displayedName = displayedName;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ProjectGroup that = (ProjectGroup) o;
		return Objects.equals(path, that.path) && Objects.equals(displayedName, that.displayedName);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(path, displayedName);
	}

	@Override
	public String toString()
	{
		return "ProjectGroup{" +
				"path='" + path + '\'' +
				", displayedName='" + displayedName + '\'' +
				'}';
	}
}
