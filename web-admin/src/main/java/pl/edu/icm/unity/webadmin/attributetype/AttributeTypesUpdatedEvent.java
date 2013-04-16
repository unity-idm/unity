/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.List;

import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.bus.Event;

/**
 * Notifies that the attribute types where updated
 * @author K. Benedyczak
 */
public class AttributeTypesUpdatedEvent implements Event
{
	private List<AttributeType> attributeTypes;

	public AttributeTypesUpdatedEvent(List<AttributeType> attributeTypes)
	{
		this.attributeTypes = attributeTypes;
	}

	public List<AttributeType> getAttributeTypes()
	{
		return attributeTypes;
	}
}
