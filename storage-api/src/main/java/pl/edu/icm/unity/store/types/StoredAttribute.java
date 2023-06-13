/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.types;

import pl.edu.icm.unity.base.attribute.AttributeExt;

/**
 * Information about stored attribute. AttributeExt plus entityId. 
 * @author K. Benedyczak
 */
public class StoredAttribute
{
	private AttributeExt attribute;
	private long entityId;

	public StoredAttribute(AttributeExt attribute, long entityId)
	{
		this.attribute = attribute;
		this.entityId = entityId;
	}
	
	public StoredAttribute(StoredAttribute toClone)
	{
		this.attribute = new AttributeExt(toClone.getAttribute());
		this.entityId = toClone.getEntityId();
	}
	
	
	public AttributeExt getAttribute()
	{
		return attribute;
	}
	
	public long getEntityId()
	{
		return entityId;
	}
	
	@Override
	public String toString()
	{
		return "StoredAttribute [attribute=" + attribute + ", entityId=" + entityId + "]";
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attribute == null) ? 0 : attribute.hashCode());
		result = prime * result + (int) (entityId ^ (entityId >>> 32));
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
		StoredAttribute other = (StoredAttribute) obj;
		if (attribute == null)
		{
			if (other.attribute != null)
				return false;
		} else if (!attribute.equals(other.attribute))
			return false;
		if (entityId != other.entityId)
			return false;
		return true;
	}
}
