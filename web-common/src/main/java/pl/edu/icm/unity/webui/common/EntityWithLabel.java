/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.basic.Entity;

/**
 * Holds {@link Entity} together with extra information useful for entity presentation in the web UI.
 * The extra information contains value of the entity's attribute designated with 
 * {@link EntityNameMetadataProvider#NAME} metadata. 
 * 
 * @author K. Benedyczak
 */
public class EntityWithLabel
{
	private Entity entity;
	private String label;
	private String txtRepresentation;
	private String shortTxtRepresentation;
	
	public EntityWithLabel(Entity entity, String label)
	{
		this.entity = entity;
		this.label = label;
		txtRepresentation = label == null ? "["+entity.getId()+"]" : label + " [" + entity.getId() + "]";
		shortTxtRepresentation = label == null ? "["+entity.getId()+"]" : label;
	}
	
	public Entity getEntity()
	{
		return entity;
	}

	public String getLabel()
	{
		return label;
	}

	/**
	 * @return entity displayed name or "[entityID]" if displayed name is undefined
	 */
	public String toShortString()
	{
		return shortTxtRepresentation;
	}

	@Override
	public String toString()
	{
		return txtRepresentation;
	}

	@Override
	public int hashCode()
	{
		return entity.hashCode();
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
		EntityWithLabel other = (EntityWithLabel) obj;
		if (entity == null)
		{
			if (other.entity != null)
				return false;
		} else if (!entity.equals(other.entity))
			return false;
		return true;
	}
}
