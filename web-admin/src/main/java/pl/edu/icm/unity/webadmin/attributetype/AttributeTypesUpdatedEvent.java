/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.Collection;

import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.bus.Event;

/**
 * Notifies that the attribute types where updated
 * @author K. Benedyczak
 */
public class AttributeTypesUpdatedEvent implements Event
{
	private Collection<AttributeType> attributeTypes;

	public AttributeTypesUpdatedEvent(Collection<AttributeType> attributeTypes)
	{
		this.attributeTypes = attributeTypes;
	}

	public Collection<AttributeType> getAttributeTypes()
	{
		return attributeTypes;
	}
}
