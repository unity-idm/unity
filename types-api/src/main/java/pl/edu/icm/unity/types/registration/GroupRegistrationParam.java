/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.Objects;

/**
 * Group registration option.
 * @author K. Benedyczak
 */
public class GroupRegistrationParam extends RegistrationParam
{
	public static enum IncludeGroupsMode
	{
		publicOnly, privateOnly, all
	}

	private String groupPath;
	private boolean multiSelect = false;
	private IncludeGroupsMode includeGroupsMode = IncludeGroupsMode.all;

	public String getGroupPath()
	{
		return groupPath;
	}

	public void setGroupPath(String groupPath)
	{
		this.groupPath = groupPath;
	}

	public boolean isMultiSelect()
	{
		return multiSelect;
	}

	public void setMultiSelect(boolean multiSelect)
	{
		this.multiSelect = multiSelect;
	}
	
	public IncludeGroupsMode getIncludeGroupsMode()
	{
		return includeGroupsMode;
	}

	public void setIncludeGroupsMode(IncludeGroupsMode includeGroupsMode)
	{
		this.includeGroupsMode = includeGroupsMode;
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof GroupRegistrationParam))
			return false;
		if (!super.equals(other))
			return false;
		GroupRegistrationParam castOther = (GroupRegistrationParam) other;
		return Objects.equals(groupPath, castOther.groupPath)
				&& Objects.equals(multiSelect, castOther.multiSelect)
				&& Objects.equals(includeGroupsMode, castOther.includeGroupsMode);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), groupPath, multiSelect, includeGroupsMode);
	}
}
