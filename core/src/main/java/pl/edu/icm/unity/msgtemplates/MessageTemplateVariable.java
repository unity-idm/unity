/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.msgtemplates;

/**
 * Represent single message template variable 
 * 
 * @author P. Piernik
 */
public class MessageTemplateVariable
{
	private String name;
	private String descriptionKey;
	private boolean mandatory;
	
	public MessageTemplateVariable(String name, String descriptionKey, boolean mandatory)
	{
		this.name = name;
		this.descriptionKey = descriptionKey;
		this.mandatory = mandatory;
	}
	public String getDescriptionKey()
	{
		return descriptionKey;
	}
	public void setDescriptionKey(String descriptionKey)
	{
		this.descriptionKey = descriptionKey;
	}
	public boolean isMandatory()
	{
		return mandatory;
	}
	public void setMandatory(boolean mandatory)
	{
		this.mandatory = mandatory;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
}
