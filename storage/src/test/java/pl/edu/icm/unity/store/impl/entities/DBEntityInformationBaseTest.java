/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.entities;

import java.util.Date;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBEntityInformationBaseTest extends DBTypeTestBase<DBEntityInformationBase>
{
	@Override
	protected String getJson()
	{
		return "{\"state\":\"valid\",\"ScheduledOperationTime\":1666788129805,\"ScheduledOperation\":\"DISABLE\",\"RemovalByUserTime\":1666788129805}";

	}

	@Override
	protected DBEntityInformationBase getObject()
	{
		return DBEntityInformationBase.builder()
				.withState("valid")
				.withRemovalByUserTime(new Date(1666788129805L))
				.withScheduledOperationTime(new Date(1666788129805L))
				.withScheduledOperation("DISABLE")
				.build();
	}
}
