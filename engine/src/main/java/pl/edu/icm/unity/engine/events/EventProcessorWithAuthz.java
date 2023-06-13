/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.events;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.event.Event;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.event.EventPublisher;
import pl.edu.icm.unity.engine.api.event.EventPublisherWithAuthz;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;

/**
 * Takes events from producers and pass it to {@link EventProcessor} after authorization check.
 * @author R. Ledzinski
 */
@Component
public class EventProcessorWithAuthz implements EventPublisherWithAuthz
{
	private EventPublisher eventPublisher;
	private InternalAuthorizationManager authz;

	@Autowired
	public EventProcessorWithAuthz(InternalAuthorizationManager authz,
								   EventPublisher eventPublisher)
	{
		this.authz = authz;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void fireEventWithAuthz(Event event) throws AuthorizationException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		eventPublisher.fireEvent(event);
	}
}
