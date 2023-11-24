/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.attribute;


import io.imunity.vaadin.endpoint.common.bus.Event;

public record AttributeChangedEvent(
		String group,
		String attributeName) implements Event
{

}
