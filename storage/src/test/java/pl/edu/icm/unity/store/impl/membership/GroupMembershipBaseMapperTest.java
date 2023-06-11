/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.membership;

import java.util.Date;
import java.util.function.Function;

import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class GroupMembershipBaseMapperTest extends MapperTestBase<GroupMembership, DBGroupMembershipBase>
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
	protected DBGroupMembershipBase getFullDBObject()
	{
		return DBGroupMembershipBase.builder()
				.withCreationTs(new Date(1))
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("profile")
				.build();
	}

	@Override
	protected Pair<Function<GroupMembership, DBGroupMembershipBase>, Function<DBGroupMembershipBase, GroupMembership>> getMapper()
	{
		return Pair.of(GroupMembershipBaseMapper::map, g -> GroupMembershipBaseMapper.map(g, "/", 1L));
	}

}
