/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.event;

import pl.edu.icm.unity.base.event.Event;

/**
 * Allows for sending platform events. 
 * 
 * @author K. Benedyczak
 */
public interface EventPublisher
{
	/**
	 * Publish the event for further processing.
	 * @param event
	 */
	void fireEvent(Event event);
}
