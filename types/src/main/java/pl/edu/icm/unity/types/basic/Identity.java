/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import java.util.List;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;

/**
 * Represents an identity with full information as returned from the engine.
 * 
 * @author K. Benedyczak
 */
public class Identity extends IdentityParam
{
	private Long entityId;
	private IdentityType type;

	private String comparableValue;
	private String prettyString;
	private String prettyStringNoPfx;
	private String ordinaryString;
	
	public Identity()
	{
	}
	
	public Identity(IdentityType type, String value, Long entityId, boolean local, String realm, String target) 
			throws IllegalIdentityValueException
	{
		super(type.getIdentityTypeProvider().getId(), value, local);
		this.entityId = entityId;
		this.type = type;
		this.type.getIdentityTypeProvider().validate(value);
		this.target = target;
		this.realm = realm;
		if (type.getIdentityTypeProvider().isTargeted() && (target == null || realm == null))
			throw new IllegalIdentityValueException("The target and realm must be set for targeted identity");
	}

	public Long getEntityId()
	{
		return entityId;
	}

	public IdentityType getType()
	{
		return type;
	}
	
	public void setEntityId(Long entityId)
	{
		this.entityId = entityId;
	}

	public void setType(IdentityType type)
	{
		this.type = type;
	}

	public List<Attribute<?>> extractAttributes()
	{
		return type.getIdentityTypeProvider().extractAttributes(getValue(), type.getExtractedAttributes());
	}
	
	public String getComparableValue()
	{
		if (comparableValue == null)
			try
			{
				comparableValue = type.getIdentityTypeProvider().getComparableValue(value, 
						realm, target);
			} catch (IllegalIdentityValueException e)
			{
				//shouldn't happen, unless somebody made a buggy code
				throw new IllegalStateException("The identity is not initialized properly", e);
			}
		return comparableValue;
	}
	
	public List<Attribute<?>> extractAllAttributes()
	{
		return type.getIdentityTypeProvider().extractAttributes(value, null);
	}
	
	/**
	 * Similar to {@link #toString()}, but allows for less verbose
	 * and more user-friendly output.
	 * @return
	 */
	public String toPrettyString()
	{
		if (prettyString == null)
			prettyString = type.getIdentityTypeProvider().toPrettyString(value);
		return prettyString;
	}

	/**
	 * Similar to {@link #toPrettyString()}, but doesn't return id type prefix.
	 * @return
	 */
	public String toPrettyStringNoPrefix()
	{
		if (prettyStringNoPfx == null)
			prettyStringNoPfx = type.getIdentityTypeProvider().toPrettyStringNoPrefix(value);
		return prettyStringNoPfx;
	}
	
	/**
	 * @return full String representation
	 */
	public String toString()
	{
		if (ordinaryString == null)
			ordinaryString = type.getIdentityTypeProvider().toString(value);
		return ordinaryString;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((getComparableValue() == null) ? 0 : getComparableValue().hashCode());
		result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		Identity other = (Identity) obj;
		if (getComparableValue() == null)
		{
			if (other.getComparableValue() != null)
				return false;
		} else if (!getComparableValue().equals(other.getComparableValue()))
			return false;
		if (entityId == null)
		{
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		if (type == null)
		{
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	
	
}
