/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Allows for flexible addressing of a subject of a method operating on a particular entity:
 * either using entityId or using {@link IdentityValue} (belonging to the entity). 
 * @author K. Benedyczak
 */
public class EntityParam
{
	private IdentityTaV identity;
	private String entityId;
	
	/**
	 * @param entityId
	 */
	public EntityParam(String entityId)
	{
		this.entityId = entityId;
	}

	public EntityParam(IdentityTaV identityValue)
	{
		this.identity = identityValue;
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
