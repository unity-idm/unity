/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.bus;

/**
 * Implemented by receivers of events.
 */
public interface EventListener<T extends Event>
{
	void handleEvent(T event);
}
