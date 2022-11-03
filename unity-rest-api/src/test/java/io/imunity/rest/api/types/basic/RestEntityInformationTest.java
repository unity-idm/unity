/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;

public class RestEntityInformationTest extends RestTypeBase<RestEntityInformation>
{
	@Override
	protected String getJson()
	{
		return "{\"state\":\"valid\",\"ScheduledOperationTime\":1666788129805,\"ScheduledOperation\":\"DISABLE\",\"RemovalByUserTime\":1666788129805,\"entityId\":1}";

	}

	@Override
	protected RestEntityInformation getObject()
	{
		return RestEntityInformation.builder().withEntityId(1L).withState("valid")
				.withRemovalByUserTime(new Date(1666788129805L)).withScheduledOperationTime(new Date(1666788129805L))
				.withScheduledOperation("DISABLE").build();
	}
}
