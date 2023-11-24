/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser;

import io.imunity.vaadin.endpoint.common.bus.Event;
import pl.edu.icm.unity.base.attribute.AttributeType;

import java.util.Collection;


public record AttributeTypesUpdatedEvent(
		Collection<AttributeType> attributeTypes) implements Event
{

}
