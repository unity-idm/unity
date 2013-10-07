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
}
