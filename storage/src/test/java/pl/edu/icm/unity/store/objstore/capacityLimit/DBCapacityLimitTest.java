/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.capacityLimit;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBCapacityLimitTest extends DBTypeTestBase<DBCapacityLimit>
{

	@Override
	protected String getJson()
	{
		return "{\"name\":\"AttributesCount\",\"value\":3}\n";
	}

	@Override
	protected DBCapacityLimit getObject()
	{
		return DBCapacityLimit.builder()
				.withName("AttributesCount")
				.withValue(3)
				.build();
	}

}
