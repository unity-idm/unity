/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.capacityLimit;

import pl.edu.icm.unity.base.capacity_limit.CapacityLimit;
import pl.edu.icm.unity.base.capacity_limit.CapacityLimitName;

class CapacityLimitMapper
{
	static DBCapacityLimit map(CapacityLimit capacityLimit)
	{
		return DBCapacityLimit.builder()
				.withName(capacityLimit.getName())
				.withValue(capacityLimit.getValue())
				.build();
	}

	static CapacityLimit map(DBCapacityLimit dbCapacityLimit)
	{
		return new CapacityLimit(CapacityLimitName.valueOf(dbCapacityLimit.name), dbCapacityLimit.value);
	}
}
