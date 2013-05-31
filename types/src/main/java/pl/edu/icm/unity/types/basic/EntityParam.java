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
	private String entityId;
	
	/**
	 * @param entityId
	 */
	public EntityParam(String entityId)
	{
		if (entityId == null)
			throw new IllegalArgumentException("Entity id can not be null");
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
		if (entityId != null)
			return;
		identity.validateInitialization();
	}

	public String getEntityId()
	{
		return entityId;
	}

	public IdentityTaV getIdentity()
	{
		return identity;
	}
}
