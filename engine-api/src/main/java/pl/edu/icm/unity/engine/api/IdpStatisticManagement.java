/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api;

import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;

public interface IdpStatisticManagement
{
	public enum GroupBy
	{
		none, day, month, total
	};
	
	List<IdpStatistic> getIdpStatisticsSince(Date since) throws EngineException;

	void deleteOlderThan(Date olderThan) throws EngineException;

	void addIdpStatistic(IdpStatistic toAdd) throws EngineException;
	
	List<GroupedIdpStatistic> getIdpStatisticsSinceGroupBy(Date since, GroupBy groupBy) throws EngineException;
	
}
