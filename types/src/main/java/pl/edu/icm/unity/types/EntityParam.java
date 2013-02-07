/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types;

/**
 * Allows for flexible addressing of a subject of a method operating on a particular entity:
 * either using entityId or using {@link IdentityValue} (belonging to the entity) 
 * or using identityId (of the entity).
 * @author K. Benedyczak
 */
public class EntityParam extends IdentityParam
{
	private String entityId;
	
	/**
	 * @param id
	 * @param entityParam allows to select whether the id is of entity (true) or identity (false)
	 */
	public EntityParam(String id, boolean entityParam)
	{
		super();
		if (entityParam)
			entityId = id;
		else
			identityId = id;
			
	}

	public EntityParam(IdentityTaV identityValue)
	{
		super(identityValue);
	}

	/**
	 * @param entityId entity id. The same as {@link #EntityParam(String, boolean)} with the second
	 * argument == true.
	 */
	public EntityParam(String entityId)
	{
		this.entityId = entityId;
	}

	public String getEntityId()
	{
		return entityId;
	}
}
