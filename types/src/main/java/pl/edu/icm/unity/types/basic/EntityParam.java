/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.basic;

import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.types.InitializationValidator;



/**
 * Allows for flexible addressing of a subject of a method operating on a particular entity:
 * either using entityId or using {@link IdentityValue} (belonging to the entity). 
 * @author K. Benedyczak
 */
public class EntityParam implements InitializationValidator
{
	private IdentityTaV identity;
	private Long entityId;
	
	/**
	 * @param entityId
	 */
	public EntityParam(Long entityId)
	{
		this.entityId = entityId;
	}

	public EntityParam(IdentityTaV identityValue)
	{
		if (identityValue == null)
			throw new IllegalArgumentException("Identity can not be null");
		this.identity = identityValue;
	}

	@Override
	public void validateInitialization() throws IllegalIdentityValueException
	{
		if (identity != null)
			identity.validateInitialization();
		else if (entityId == null)
			throw new IllegalIdentityValueException("Either identityTaV or entityId must be set in entityParam");
	}

	public Long getEntityId()
	{
		return entityId;
	}

	public IdentityTaV getIdentity()
	{
		return identity;
	}
	
	@Override
	public String toString()
	{
		return entityId == null ? identity.toString() : entityId.toString();
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
		result = prime * result + ((identity == null) ? 0 : identity.hashCode());
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
		EntityParam other = (EntityParam) obj;
		if (entityId == null)
		{
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		if (identity == null)
		{
			if (other.identity != null)
				return false;
		} else if (!identity.equals(other.identity))
			return false;
		return true;
	}
}
