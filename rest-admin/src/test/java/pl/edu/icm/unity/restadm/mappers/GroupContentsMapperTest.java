/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestGroup;
import io.imunity.rest.api.types.basic.RestGroupContents;
import io.imunity.rest.api.types.basic.RestGroupDelegationConfiguration;
import io.imunity.rest.api.types.basic.RestGroupMembership;
import io.imunity.rest.api.types.basic.RestI18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;

public class GroupContentsMapperTest extends MapperTestBase<GroupContents, RestGroupContents>
{
	@Override
	protected GroupContents getAPIObject()
	{
		GroupContents groupContents = new GroupContents();
		groupContents.setGroup(new Group("/A"));
		groupContents.setMembers(List.of(new GroupMembership("/A", 1, new Date(1))));
		groupContents.setSubGroups(List.of("/A/B"));
		return groupContents;
	}

	@Override
	protected RestGroupContents getRestObject()
	{
		return RestGroupContents.builder()
				.withGroup(RestGroup.builder()
						.withDelegationConfiguration(RestGroupDelegationConfiguration.builder()
								.withEnabled(false)
								.build())
						.withPath("/A")
						.withDisplayedName(RestI18nString.builder()
								.withDefaultValue("/A")
								.build())
						.withI18nDescription(RestI18nString.builder()
								.build())
						.build())
				.withMembers(List.of(RestGroupMembership.builder()
						.withGroup("/A")
						.withEntityId(1)
						.withCreationTs(new Date(1))
						.build()))
				.withSubGroups(List.of("/A/B"))
				.build();
	}

	@Override
	protected Pair<Function<GroupContents, RestGroupContents>, Function<RestGroupContents, GroupContents>> getMapper()
	{
		return Pair.of(GroupContentsMapper::map, GroupContentsMapper::map);
	}

}
