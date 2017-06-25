/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Group registration option.
 * @author K. Benedyczak
 */
public class GroupRegistrationParam extends RegistrationParam
{
	private String groupPath;

	public String getGroupPath()
	{
		return groupPath;
	}

	public void setGroupPath(String groupPath)
	{
		this.groupPath = groupPath;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((groupPath == null) ? 0 : groupPath.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		GroupRegistrationParam other = (GroupRegistrationParam) obj;
		if (groupPath == null)
		{
			if (other.groupPath != null)
				return false;
		} else if (!groupPath.equals(other.groupPath))
			return false;
		return true;
	}
}
