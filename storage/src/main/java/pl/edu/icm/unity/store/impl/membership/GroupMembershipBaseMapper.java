/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.membership;

import pl.edu.icm.unity.base.group.GroupMembership;

class GroupMembershipBaseMapper
{
	static DBGroupMembershipBase map(GroupMembership groupMembership)
	{
		return DBGroupMembershipBase.builder()
				.withCreationTs(groupMembership.getCreationTs())
				.withRemoteIdp(groupMembership.getRemoteIdp())
				.withTranslationProfile(groupMembership.getTranslationProfile())
				.build();
	}

	static GroupMembership map(DBGroupMembershipBase restGroupMembership, String group, long entityId)
	{
		return new GroupMembership(group, entityId, restGroupMembership.creationTs,
				restGroupMembership.translationProfile, restGroupMembership.remoteIdp);
	}
}
