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
}
