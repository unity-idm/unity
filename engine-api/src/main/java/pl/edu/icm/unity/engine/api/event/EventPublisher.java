/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
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
	void fireEvent(Event event);
}
