/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Assignment of attribute class in a particular group.  
 * @author K. Benedyczak
 */
public class AttributeClassAssignment
{
	private String acName;
	private String group;

	public String getAcName()
	{
		return acName;
	}
	public void setAcName(String acName)
	{
		this.acName = acName;
	}
	public String getGroup()
	{
		return group;
	}
	public void setGroup(String group)
	{
		this.group = group;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((acName == null) ? 0 : acName.hashCode());
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributeClassAssignment other = (AttributeClassAssignment) obj;
		if (acName == null)
		{
			if (other.acName != null)
				return false;
		} else if (!acName.equals(other.acName))
			return false;
		if (group == null)
		{
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		return true;
	}
}
