/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.event;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.types.Event;

/**
 * Allows for sending platform events. 
 * 
 * @author R. Ledzinski
 */
public interface EventPublisherWithAuthz
{
	/**
	 * Invokes the event thru {@link EventPublisher.fireEvent(Event)}, however the caller is authorized
	 * with the highest privilege level.
	 * @param event
	 * @throws AuthorizationException 
	 */
	void fireEventWithAuthz(Event event) throws AuthorizationException;
}
