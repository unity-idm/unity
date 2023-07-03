/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.capacityLimit;

import java.util.function.Function;

import pl.edu.icm.unity.base.capacity_limit.CapacityLimit;
import pl.edu.icm.unity.base.capacity_limit.CapacityLimitName;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class CapacityLimitMapperTest extends MapperTestBase<CapacityLimit, DBCapacityLimit>
{

	@Override
	protected CapacityLimit getFullAPIObject()
	{
		return new CapacityLimit(CapacityLimitName.AttributesCount, 3);
	}

	@Override
	protected DBCapacityLimit getFullDBObject()
	{

		return DBCapacityLimit.builder()
				.withName("AttributesCount")
				.withValue(3)
				.build();
	}

	@Override
	protected Pair<Function<CapacityLimit, DBCapacityLimit>, Function<DBCapacityLimit, CapacityLimit>> getMapper()
	{
		return Pair.of(CapacityLimitMapper::map, CapacityLimitMapper::map);
	}

}
