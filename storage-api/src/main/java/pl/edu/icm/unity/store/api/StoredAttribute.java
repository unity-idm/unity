/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.api;

import pl.edu.icm.unity.types.basic.AttributeExt;

/**
 * Information about stored attribute. AttributeExt plus entityId. 
 * @author K. Benedyczak
 */
public class StoredAttribute
{
	private AttributeExt<?> attribute;
	private long entityId;

	public StoredAttribute(AttributeExt<?> attribute, long entityId)
	{
		super();
		this.attribute = attribute;
		this.entityId = entityId;
	}
	public AttributeExt<?> getAttribute()
	{
		return attribute;
	}
	public long getEntityId()
	{
		return entityId;
	}
}
