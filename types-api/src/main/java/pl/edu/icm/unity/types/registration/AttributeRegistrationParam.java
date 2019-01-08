/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Attribute registration option.
 * @author K. Benedyczak
 */
public class AttributeRegistrationParam extends OptionalRegistrationParam
{
	public static final String DYN_GROUP_PFX = "DYN:";
	private String attributeType;
	private String group;
	private boolean showGroups;
	private boolean useDescription;
	private ConfirmationMode confirmationMode = ConfirmationMode.ON_SUBMIT;
	
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
	@JsonIgnore
	public boolean isUsingDynamicGroup()
	{
		return group.startsWith(DYN_GROUP_PFX);
	}
	@JsonIgnore
	public String getDynamicGroup()
	{
		return group.substring(DYN_GROUP_PFX.length());
	}
	public boolean isShowGroups()
	{
		return showGroups;
	}
	public void setShowGroups(boolean showGroups)
	{
		this.showGroups = showGroups;
	}
	public ConfirmationMode getConfirmationMode()
	{
		return confirmationMode;
	}
	public void setConfirmationMode(ConfirmationMode confirmationMode)
	{
		this.confirmationMode = confirmationMode;
	}
	/**
	 * @deprecated do not use, the feature is disabled, the attribute type's description is always used, 
	 * unless in legacy form where it is overridden with a fixed value in the form. The method is left
	 * as it may still be used during legacy objects deserialization from JSON.
	 * @return
	 */
	@Deprecated
	public boolean isUseDescription()
	{
		return useDescription;
	}
	
	/**
	 * @deprecated see {@link #isUseDescription()}
	 * @param useDescription
	 */
	@Deprecated
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
		result = prime * result
				+ ((confirmationMode == null) ? 0 : confirmationMode.hashCode());
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
		if (confirmationMode != other.confirmationMode)
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
