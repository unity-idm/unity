/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp.statistic;

import org.springframework.context.ApplicationEventPublisher;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.idpStatistic.IdpStatistic.Status;
import pl.edu.icm.unity.base.message.MessageSource;

public class IdpStatisticReporter
{
	private final ApplicationEventPublisher applicationEventPublisher;
	protected final MessageSource msg;
	private final Endpoint endpoint;

	public IdpStatisticReporter(ApplicationEventPublisher applicationEventPublisher, MessageSource msg,
			Endpoint endpoint)
	{
		this.applicationEventPublisher = applicationEventPublisher;
		this.msg = msg;
		this.endpoint = endpoint;
	}

	public void reportStatus(String clientId, String clientName, Status status)
	{
		applicationEventPublisher.publishEvent(new IdpStatisticEvent(endpoint.getName(),
				endpoint.getConfiguration().getDisplayedName() != null
						? endpoint.getConfiguration().getDisplayedName().getValue(msg)
						: null,
				clientId, clientName, status));
	}
}
