/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance;

import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement.GroupBy;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Component
class IdPUsageStatisticsController
{
	private final IdpStatisticManagement idpStatisticManagement;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	IdPUsageStatisticsController(IdpStatisticManagement idpStatisticManagement, MessageSource msg, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.idpStatisticManagement = idpStatisticManagement;
		this.notificationPresenter = notificationPresenter;
	}

	Collection<GroupedIdpStatistic> getIdpStatistics(LocalDateTime since)
	{
		try
		{
			return idpStatisticManagement.getIdpStatisticsSinceGroupBy(since, GroupBy.total,
					IdpStatisticManagement.DEFAULT_SIG_IN_RECORD_LIMIT, false);
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("IdpStatisticsController.getError"), e.getMessage());
			return List.of();
		}

	}

	void drop(LocalDateTime since)
	{
		try
		{
			idpStatisticManagement.deleteOlderThan(since);

		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("IdpStatisticsController.cannotDropStatistics"), e.getMessage());
		}
	}
}
