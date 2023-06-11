/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.membership;

import pl.edu.icm.unity.base.group.GroupMembership;

class GroupMembershipMapper
{
	static DBGroupMembership map(GroupMembership groupMembership)
	{
		return DBGroupMembership.builder()
				.withCreationTs(groupMembership.getCreationTs())
				.withEntityId(groupMembership.getEntityId())
				.withGroup(groupMembership.getGroup())
				.withRemoteIdp(groupMembership.getRemoteIdp())
				.withTranslationProfile(groupMembership.getTranslationProfile())
				.build();
	}

	static GroupMembership map(DBGroupMembership restGroupMembership)
	{
		return new GroupMembership(restGroupMembership.group, restGroupMembership.entityId,
				restGroupMembership.creationTs, restGroupMembership.translationProfile, restGroupMembership.remoteIdp);
	}
}
