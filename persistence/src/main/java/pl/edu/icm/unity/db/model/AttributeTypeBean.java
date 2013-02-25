/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.db.model;

/**
 * In DB attribute type representation.
 * @author K. Benedyczak
 */
public class AttributeTypeBean extends BaseBean
{
	private String valueSyntaxId;

	public AttributeTypeBean()
	{
	}
	
	public AttributeTypeBean(String name, byte[] contents, String valueSyntaxId)
	{
		super(name, contents);
		this.valueSyntaxId = valueSyntaxId;
	}


	public String getValueSyntaxId()
	{
		return valueSyntaxId;
	}

	public void setValueSyntaxId(String valueSyntaxId)
	{
		this.valueSyntaxId = valueSyntaxId;
	}
}
