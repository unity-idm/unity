/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers;

import io.imunity.rest.api.types.basic.RestGroupProperty;
import pl.edu.icm.unity.types.basic.GroupProperty;

public class GroupPropertyMapper
{
	static RestGroupProperty map(GroupProperty groupProperty)
	{
		return RestGroupProperty.builder()
				.withKey(groupProperty.key)
				.withValue(groupProperty.value)
				.build();
	}

	static GroupProperty map(RestGroupProperty groupProperty)
	{
		return new GroupProperty(groupProperty.key, groupProperty.value);
	}
}
