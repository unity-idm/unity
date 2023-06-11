/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.function.Function;

import pl.edu.icm.unity.base.group.GroupProperty;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class GroupPropertyMapperTest extends MapperTestBase<GroupProperty, DBGroupProperty>
{

	@Override
	protected GroupProperty getFullAPIObject()
	{
		return new GroupProperty("key", "value");
	}

	@Override
	protected DBGroupProperty getFullDBObject()
	{
		return DBGroupProperty.builder()
				.withKey("key")
				.withValue("value")
				.build();
	}

	@Override
	protected Pair<Function<GroupProperty, DBGroupProperty>, Function<DBGroupProperty, GroupProperty>> getMapper()
	{
		return Pair.of(GroupPropertyMapper::map, GroupPropertyMapper::map);
	}

}
