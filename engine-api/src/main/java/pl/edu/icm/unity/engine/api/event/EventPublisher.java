/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.event;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.types.AbstractEvent;

/**
 * Allows for sending platform events. 
 * 
 * @author K. Benedyczak
 */
public interface EventPublisher
{
	/**
	 * Invokes the event as {@link #fireEvent(AbstractEvent)}, however the caller is authorized
	 * with the highest privilege level.
	 * @param event
	 * @throws AuthorizationException 
	 */
	void fireEventWithAuthz(AbstractEvent event) throws AuthorizationException;

	void fireEvent(AbstractEvent event);
}
