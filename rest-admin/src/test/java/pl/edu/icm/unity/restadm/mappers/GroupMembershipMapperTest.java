/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Date;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestGroupMembership;
import pl.edu.icm.unity.types.basic.GroupMembership;

public class GroupMembershipMapperTest extends MapperTestBase<GroupMembership, RestGroupMembership>
{

	@Override
	protected GroupMembership getAPIObject()
	{
		GroupMembership groupMembership = new GroupMembership("/", 1, new Date(1));
		groupMembership.setTranslationProfile("profile");
		groupMembership.setRemoteIdp("remoteIdp");
		return groupMembership;
	}

	@Override
	protected RestGroupMembership getRestObject()
	{
		return RestGroupMembership.builder()
				.withGroup("/")
				.withEntityId(1)
				.withCreationTs(new Date(1))
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("profile")
				.build();
	}

	@Override
	protected Pair<Function<GroupMembership, RestGroupMembership>, Function<RestGroupMembership, GroupMembership>> getMapper()
	{
		return Pair.of(GroupMembershipMapper::map, GroupMembershipMapper::map);
	}

}
