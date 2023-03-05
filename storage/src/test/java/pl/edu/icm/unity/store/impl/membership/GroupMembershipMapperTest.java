/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.membership;

import java.util.Date;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.basic.GroupMembership;

public class GroupMembershipMapperTest extends MapperWithMinimalTestBase<GroupMembership, DBGroupMembership>
{

	@Override
	protected GroupMembership getFullAPIObject()
	{
		GroupMembership groupMembership = new GroupMembership("/", 1, new Date(1));
		groupMembership.setTranslationProfile("profile");
		groupMembership.setRemoteIdp("remoteIdp");
		return groupMembership;
	}

	@Override
	protected DBGroupMembership getFullDBObject()
	{
		return DBGroupMembership.builder()
				.withGroup("/")
				.withEntityId(1)
				.withCreationTs(new Date(1))
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("profile")
				.build();
	}

	@Override
	protected GroupMembership getMinAPIObject()
	{

		return new GroupMembership("/", 1, new Date(1));
	}

	@Override
	protected DBGroupMembership getMinDBObject()
	{

		return DBGroupMembership.builder()
				.withGroup("/")
				.withEntityId(1)
				.withCreationTs(new Date(1))
				.build();
	}

	@Override
	protected Pair<Function<GroupMembership, DBGroupMembership>, Function<DBGroupMembership, GroupMembership>> getMapper()
	{
		return Pair.of(GroupMembershipMapper::map, GroupMembershipMapper::map);
	}

}
