/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser;

import pl.edu.icm.unity.base.entity.Entity;


public class EntityWithLabel
{
	private final Entity entity;
	private final String label;
	private final String txtRepresentation;
	private final String shortTxtRepresentation;
	
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
