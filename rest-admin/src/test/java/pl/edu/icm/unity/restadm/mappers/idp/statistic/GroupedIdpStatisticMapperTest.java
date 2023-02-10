/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.idp.statistic;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.idp.statistic.RestGroupedIdpStatistic;
import io.imunity.rest.api.types.idp.statistic.RestSigInStatistic;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic.SigInStatistic;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;

public class GroupedIdpStatisticMapperTest extends MapperTestBase<GroupedIdpStatistic, RestGroupedIdpStatistic>
{
	@Override
	protected GroupedIdpStatistic getFullAPIObject()
	{
		SigInStatistic sigInStatistic = new SigInStatistic(
				LocalDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneId.of("UTC"))
						.truncatedTo(ChronoUnit.SECONDS),
				LocalDateTime.ofInstant(Instant.ofEpochMilli(999999), ZoneId.of("UTC"))
						.truncatedTo(ChronoUnit.SECONDS),
				1, 2, 3);

		return new GroupedIdpStatistic("idpId", "name", "client", "clientN", List.of(sigInStatistic));
	}

	@Override
	protected RestGroupedIdpStatistic getFullRestObject()
	{
		return RestGroupedIdpStatistic.builder()
				.withClientId("client")
				.withClientName("clientN")
				.withIdpId("idpId")
				.withIdpName("name")
				.withSigInStats(List.of(RestSigInStatistic.builder()
						.withPeriodStart(LocalDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneId.of("UTC"))
								.truncatedTo(ChronoUnit.SECONDS))
						.withPeriodEnd(LocalDateTime.ofInstant(Instant.ofEpochMilli(999999), ZoneId.of("UTC"))
								.truncatedTo(ChronoUnit.SECONDS))
						.withSuccessfullCount(2)
						.withTotatCount(1)
						.withFailedCount(3)
						.build()))
				.build();
	}

	@Override
	protected Pair<Function<GroupedIdpStatistic, RestGroupedIdpStatistic>, Function<RestGroupedIdpStatistic, GroupedIdpStatistic>> getMapper()
	{
		return Pair.of(GroupedIdpStatisticMapper::map, GroupedIdpStatisticMapper::map);
	}

}
