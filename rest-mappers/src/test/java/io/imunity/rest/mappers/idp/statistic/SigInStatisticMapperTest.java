/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.idp.statistic;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import io.imunity.rest.api.types.idp.statistic.RestSigInStatistic;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic.SigInStatistic;

public class SigInStatisticMapperTest extends MapperTestBase<SigInStatistic, RestSigInStatistic>
{

	@Override
	protected SigInStatistic getFullAPIObject()
	{

		return new SigInStatistic(LocalDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneId.of("UTC"))
				.truncatedTo(ChronoUnit.SECONDS),
				LocalDateTime.ofInstant(Instant.ofEpochMilli(999999), ZoneId.of("UTC"))
						.truncatedTo(ChronoUnit.SECONDS),
				1, 2, 3);
	}

	@Override
	protected RestSigInStatistic getFullRestObject()
	{
		return RestSigInStatistic.builder()
				.withPeriodStart(LocalDateTime.ofInstant(Instant.ofEpochMilli(1), ZoneId.of("UTC"))
						.truncatedTo(ChronoUnit.SECONDS))
				.withPeriodEnd(LocalDateTime.ofInstant(Instant.ofEpochMilli(999999), ZoneId.of("UTC"))
						.truncatedTo(ChronoUnit.SECONDS))
				.withSuccessfullCount(2)
				.withTotatCount(1)
				.withFailedCount(3)
				.build();
	}

	@Override
	protected Pair<Function<SigInStatistic, RestSigInStatistic>, Function<RestSigInStatistic, SigInStatistic>> getMapper()
	{
		return Pair.of(SigInStatisticMapper::map, SigInStatisticMapper::map);
	}

}
