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
}
