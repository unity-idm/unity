/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.idpStatistic;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.endpoint.idp.IdpStatistic;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.IdpStatisticManagement;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.engine.authz.AuthzCapability;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManager;
import pl.edu.icm.unity.store.api.IdpStatisticDAO;
import pl.edu.icm.unity.store.api.tx.Transactional;

@Component
public class IdpStatisticManagementImpl implements IdpStatisticManagement
{
	private final IdpStatisticDAO dao;
	private final InternalAuthorizationManager authz;

	public IdpStatisticManagementImpl(IdpStatisticDAO dao, InternalAuthorizationManager authz)
	{
		this.dao = dao;
		this.authz = authz;
	}

	@Override
	@Transactional
	public List<IdpStatistic> getIdpStatisticsSince(LocalDateTime since, int limit) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		return dao.getIdpStatistics(since, LocalDateTime.now(), limit);
	}

	@Override
	@Transactional
	public void deleteOlderThan(LocalDateTime olderThan) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		dao.deleteOlderThan(olderThan);
	}

	@Override
	@Transactional
	public void addIdpStatistic(IdpStatistic toAdd) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		dao.create(toAdd);
	}

	@Override
	@Transactional
	public List<GroupedIdpStatistic> getIdpStatisticsSinceGroupBy(LocalDateTime since, GroupBy groupBy,
			int sigInRecordlimit, boolean skipZerosRecords) throws EngineException
	{
		authz.checkAuthorization(AuthzCapability.maintenance);
		List<IdpStatistic> idpStatistics = dao.getIdpStatistics(since, LocalDateTime.now(), Integer.MAX_VALUE);
		return IdpStatisticGroupingHelper.groupBy(since, idpStatistics, groupBy, sigInRecordlimit, skipZerosRecords);
	}
}
