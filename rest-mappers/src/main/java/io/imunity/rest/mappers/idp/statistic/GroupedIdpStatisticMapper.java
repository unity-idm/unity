/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.idp.statistic;

import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.idp.statistic.RestGroupedIdpStatistic;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;

public class GroupedIdpStatisticMapper
{
	public static RestGroupedIdpStatistic map(GroupedIdpStatistic groupedIdpStatistic)
	{
		return RestGroupedIdpStatistic.builder()
				.withClientId(groupedIdpStatistic.clientId)
				.withClientName(groupedIdpStatistic.clientName)
				.withIdpId(groupedIdpStatistic.idpId)
				.withIdpName(groupedIdpStatistic.idpName)
				.withSigInStats(Optional.ofNullable(groupedIdpStatistic.sigInStats)
						.map(s -> s.stream()
								.map(sigInStat -> Optional.ofNullable(sigInStat)
										.map(SigInStatisticMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null))
				.build();
	}

	public static GroupedIdpStatistic map(RestGroupedIdpStatistic restGroupedIdpStatistic)
	{
		return new GroupedIdpStatistic(restGroupedIdpStatistic.idpId, restGroupedIdpStatistic.idpName,
				restGroupedIdpStatistic.clientId, restGroupedIdpStatistic.clientName,
				Optional.ofNullable(restGroupedIdpStatistic.sigInStats)
						.map(s -> s.stream()
								.map(sigInStat -> Optional.ofNullable(sigInStat)
										.map(SigInStatisticMapper::map)
										.orElse(null))
								.collect(Collectors.toList()))
						.orElse(null));
	}

}
