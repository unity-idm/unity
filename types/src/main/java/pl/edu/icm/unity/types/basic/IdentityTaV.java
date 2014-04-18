/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.InitializationValidator;


/**
 * Represents an identity type and value. This class is useful to address existing identity as a parameter.
 * <p>
 * Optionally a target can be set. Then the identity can be resolved for the specified receiver.
 * 
 * @author K. Benedyczak
 */
public class IdentityTaV implements InitializationValidator
{
	private String typeId;
	protected String value;
	protected String target;
	
	
	public IdentityTaV()
	{
	}
	
	public IdentityTaV(String type, String value) 
	{
		this.typeId = type;
		this.value = value;
	}

	public IdentityTaV(String type, String value, String target) 
	{
		this(type, value);
		this.target = target;
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

	public String getTarget()
	{
		return target;
	}

	public void setTarget(String target)
	{
		this.target = target;
	}

	@Override
	public void validateInitialization() throws IllegalIdentityValueException
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

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		result = prime * result + ((typeId == null) ? 0 : typeId.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		IdentityTaV other = (IdentityTaV) obj;
		if (target == null)
		{
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		if (typeId == null)
		{
			if (other.typeId != null)
				return false;
		} else if (!typeId.equals(other.typeId))
			return false;
		if (value == null)
		{
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
}
