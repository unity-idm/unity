/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;


/**
 * Represents an identity type and value. This class is useful to address existing identity as a parameter.
 * 
 * @author K. Benedyczak
 */
public class IdentityTaV implements InitializationValidator
{
	private String typeId;
	protected String value;
	
	public IdentityTaV()
	{
	}
	
	public IdentityTaV(String type, String value) 
	{
		this.typeId = type;
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}
	
	public String getTypeId()
	{
		return typeId;
	}

	public void setTypeId(String type)
	{
		this.typeId = type;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	@Override
	public void validateInitialization()
	{
		if (typeId == null)
			throw new IllegalIdentityValueException("Identity type must be set");
		if (value == null)
			throw new IllegalIdentityValueException("Identity value must be set");
	}
	
	/**
	 * @return full String representation
	 */
	public String toString()
	{
		return "[" + typeId + "] " + value;
	}
}
