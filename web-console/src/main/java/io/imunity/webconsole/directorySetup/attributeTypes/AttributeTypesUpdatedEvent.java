/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directorySetup.attributeTypes;

import java.util.Collection;

import pl.edu.icm.unity.base.attribute.AttributeType;
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
