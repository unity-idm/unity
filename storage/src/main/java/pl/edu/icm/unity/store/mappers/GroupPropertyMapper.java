/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.mappers;

import pl.edu.icm.unity.store.types.DBGroupProperty;
import pl.edu.icm.unity.types.basic.GroupProperty;

public class GroupPropertyMapper
{
	static DBGroupProperty map(GroupProperty groupProperty)
	{
		return DBGroupProperty.builder()
				.withKey(groupProperty.key)
				.withValue(groupProperty.value)
				.build();
	}

	static GroupProperty map(DBGroupProperty groupProperty)
	{
		return new GroupProperty(groupProperty.key, groupProperty.value);
	}
}
