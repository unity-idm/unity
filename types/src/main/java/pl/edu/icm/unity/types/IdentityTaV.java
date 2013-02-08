/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import java.util.List;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;

/**
 * Represents an identity type and value. It offers a lot of functionality, which is implemented
 * by a {@link IdentityTypeDefinition}.
 * 
 * @author K. Benedyczak
 */
public class IdentityTaV
{
	private IdentityTypeDefinition type;
	private String value;
	
	private String comparableValue;
	private String prettyString;
	private String prettyStringNoPfx;
	private String ordinaryString;
	
	public IdentityTaV(IdentityTypeDefinition type, String value) throws IllegalIdentityValueException
	{
		this.type = type;
		this.value = value;
		this.type.validate(value);
	}

	public String getValue()
	{
		return value;
	}
	
	public IdentityTypeDefinition getTypeDefinition()
	{
		return type;
	}

	
	public String getComparableValue()
	{
		if (comparableValue == null)
			comparableValue = type.getComparableValue(value);
		return comparableValue;
	}
	
	public List<Attribute<?>> extractAllAttributes()
	{
		return type.extractAttributes(value, null);
	}
	
	/**
	 * Similar to {@link #toString()}, but allows for less verbose
	 * and more user-friendly output.
	 * @return
	 */
	public String toPrettyString()
	{
		if (prettyString == null)
			prettyString = type.toPrettyString(value);
		return prettyString;
	}

	/**
	 * Similar to {@link #toPrettyString()}, but doesn't return id type prefix.
	 * @return
	 */
	public String toPrettyStringNoPrefix()
	{
		if (prettyStringNoPfx == null)
			prettyStringNoPfx = type.toPrettyStringNoPrefix(value);
		return prettyStringNoPfx;
	}

	/**
	 * @return full String representation
	 */
	public String toString()
	{
		if (ordinaryString == null)
			ordinaryString = type.toString(value);
		return ordinaryString;
	}
}
