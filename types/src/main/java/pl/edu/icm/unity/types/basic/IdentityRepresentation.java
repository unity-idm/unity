/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

/**
 * Data required to store identity in database.
 * @author K. Benedyczak
 */
public class IdentityRepresentation
{
	private String comparableValue;
	private String contents;
	
	public IdentityRepresentation(String comparableValue, String contents)
	{
		super();
		this.comparableValue = comparableValue;
		this.contents = contents;
	}

	public String getComparableValue()
	{
		return comparableValue;
	}
	
	public void setComparableValue(String comparableValue)
	{
		this.comparableValue = comparableValue;
	}
	
	public String getContents()
	{
		return contents;
	}
	
	public void setContents(String contents)
	{
		this.contents = contents;
	}
}