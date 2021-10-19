/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api;

import java.time.LocalDateTime;
import java.util.List;

import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.idpStatistic.IdpStatistic;

public interface IdpStatisticManagement
{
	public static final int DEFAULT_STAT_SIZE_LIMIT = Integer.MAX_VALUE;

	
	public enum GroupBy
	{
		none, day, month, total
	};
	
	List<IdpStatistic> getIdpStatisticsSince(LocalDateTime since,  int limit) throws EngineException;

	void deleteOlderThan(LocalDateTime olderThan) throws EngineException;

	void addIdpStatistic(IdpStatistic toAdd) throws EngineException;
	
	List<GroupedIdpStatistic> getIdpStatisticsSinceGroupBy(LocalDateTime since, GroupBy groupBy,  int limit) throws EngineException;
	
}
