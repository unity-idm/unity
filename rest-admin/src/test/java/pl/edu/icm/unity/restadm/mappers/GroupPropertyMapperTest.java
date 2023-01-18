/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestGroupProperty;
import pl.edu.icm.unity.types.basic.GroupProperty;

public class GroupPropertyMapperTest extends MapperTestBase<GroupProperty, RestGroupProperty>
{

	@Override
	protected GroupProperty getAPIObject()
	{
		return new GroupProperty("key", "value");
	}

	@Override
	protected RestGroupProperty getRestObject()
	{
		return RestGroupProperty.builder()
				.withKey("key")
				.withValue("value")
				.build();
	}

	@Override
	protected Pair<Function<GroupProperty, RestGroupProperty>, Function<RestGroupProperty, GroupProperty>> getMapper()
	{
		return Pair.of(GroupPropertyMapper::map, GroupPropertyMapper::map);
	}

}
