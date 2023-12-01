/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.idp.statistic;

import io.imunity.rest.api.types.idp.statistic.RestSigInStatistic;
import pl.edu.icm.unity.engine.api.idp.statistic.GroupedIdpStatistic.SigInStatistic;

public class SigInStatisticMapper
{
	static RestSigInStatistic map(SigInStatistic sigInStatistic)
	{
		return RestSigInStatistic.builder()
				.withFailedCount(sigInStatistic.failedCount)
				.withSuccessfullCount(sigInStatistic.successfullCount)
				.withTotatCount(sigInStatistic.totatCount)
				.withPeriodStart(sigInStatistic.periodStart)
				.withPeriodEnd(sigInStatistic.periodEnd)
				.build();
	}

	static SigInStatistic map(RestSigInStatistic sigInStatistic)
	{
		return new SigInStatistic(sigInStatistic.periodStart, sigInStatistic.periodEnd, sigInStatistic.totatCount,
				sigInStatistic.successfullCount, sigInStatistic.failedCount);
	}
}
