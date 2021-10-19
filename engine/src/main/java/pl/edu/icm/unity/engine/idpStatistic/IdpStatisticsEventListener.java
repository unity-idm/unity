/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.idpStatistic;

import java.time.LocalDateTime;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.idp.statistic.IdpStatisticEvent;
import pl.edu.icm.unity.store.api.IdpStatisticDAO;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;

@Component
class IdpStatisticsEventListener
{
	private final IdpStatisticDAO dao;
	private final TransactionalRunner tx;

	public IdpStatisticsEventListener(IdpStatisticDAO dao, TransactionalRunner tx)
	{
		this.dao = dao;
		this.tx = tx;
	}

	@EventListener
	@Async
	void handleEvent(IdpStatisticEvent event)
	{
		tx.runInTransaction(() ->
		{
			dao.create(IdpStatistic.builder().idpEndpointId(event.idpEndpointId).idpEndpointName(event.idpEndpointName)
					.clientId(event.clientId).clientName(event.clientName).status(event.status)
					.timestamp(LocalDateTime.now()).build());
		});
	}
}
