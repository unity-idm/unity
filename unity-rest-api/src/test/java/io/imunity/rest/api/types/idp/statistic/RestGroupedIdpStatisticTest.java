/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.idp.statistic;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestGroupedIdpStatisticTest extends RestTypeBase<RestGroupedIdpStatistic>
{

	@Override
	protected String getJson()
	{
		return "{\"idpId\":\"idpId\",\"idpName\":\"name\",\"clientId\":\"client\","
				+ "\"clientName\":\"clientN\",\"sigInStats\":[{\"periodStart\":\"1970-01-01T00:00:00Z\","
				+ "\"periodEnd\":\"1970-01-01T00:16:39Z\",\"totatCount\":1,\"successfullCount\":2,\"failedCount\":3}]}\n";
	}

	@Override
	protected RestGroupedIdpStatistic getObject()
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

}
