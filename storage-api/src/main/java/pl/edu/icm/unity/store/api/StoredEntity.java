/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import pl.edu.icm.unity.types.EntityInformation;

/**
 * Represents core information that is stored about a particular entity in the system. Does not 
 * include anything derived about entity as its identities.
 * @author K. Benedyczak
 */
public class StoredEntity
{
	private Long id;
	private EntityInformation entityInformation;
	
	
	public StoredEntity(Long id, EntityInformation entityInformation)
	{
		this.id = id;
		this.entityInformation = entityInformation;
	}
	
	public Long getId()
	{
		return id;
	}
	public void setId(Long id)
	{
		this.id = id;
	}
	public EntityInformation getEntityInformation()
	{
		return entityInformation;
	}
	public void setEntityInformation(EntityInformation entityInformation)
	{
		this.entityInformation = entityInformation;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((entityInformation == null) ? 0 : entityInformation.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		StoredEntity other = (StoredEntity) obj;
		if (entityInformation == null)
		{
			if (other.entityInformation != null)
				return false;
		} else if (!entityInformation.equals(other.entityInformation))
			return false;
		if (id == null)
		{
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
