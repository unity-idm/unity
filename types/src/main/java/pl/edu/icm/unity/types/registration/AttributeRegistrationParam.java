/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

/**
 * Attribute registration option.
 * @author K. Benedyczak
 */
public class AttributeRegistrationParam extends RegistrationParam
{
	private String attributeType;
	private String group;
	private boolean showGroups;
	private boolean useDescription;
	public String getAttributeType()
	{
		return attributeType;
	}
	public void setAttributeType(String attributeType)
	{
		this.attributeType = attributeType;
	}
	public String getGroup()
	{
		return group;
	}
	public void setGroup(String group)
	{
		this.group = group;
	}
	public boolean isShowGroups()
	{
		return showGroups;
	}
	public void setShowGroups(boolean showGroups)
	{
		this.showGroups = showGroups;
	}
	public boolean isUseDescription()
	{
		return useDescription;
	}
	public void setUseDescription(boolean useDescription)
	{
		this.useDescription = useDescription;
	}
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((attributeType == null) ? 0 : attributeType.hashCode());
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + (showGroups ? 1231 : 1237);
		result = prime * result + (useDescription ? 1231 : 1237);
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
		AttributeRegistrationParam other = (AttributeRegistrationParam) obj;
		if (attributeType == null)
		{
			if (other.attributeType != null)
				return false;
		} else if (!attributeType.equals(other.attributeType))
			return false;
		if (group == null)
		{
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		if (showGroups != other.showGroups)
			return false;
		if (useDescription != other.useDescription)
			return false;
		return true;
	}
}
