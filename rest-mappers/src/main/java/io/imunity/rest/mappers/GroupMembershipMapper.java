/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers;

import io.imunity.rest.api.types.basic.RestGroupMembership;
import pl.edu.icm.unity.base.group.GroupMembership;

public class GroupMembershipMapper
{
	public static RestGroupMembership map(GroupMembership groupMembership)
	{
		return RestGroupMembership.builder()
				.withCreationTs(groupMembership.getCreationTs())
				.withEntityId(groupMembership.getEntityId())
				.withGroup(groupMembership.getGroup())
				.withRemoteIdp(groupMembership.getRemoteIdp())
				.withTranslationProfile(groupMembership.getTranslationProfile())
				.build();
	}

	static GroupMembership map(RestGroupMembership restGroupMembership)
	{
		return new GroupMembership(restGroupMembership.group, restGroupMembership.entityId,
				restGroupMembership.creationTs, restGroupMembership.translationProfile, restGroupMembership.remoteIdp);
	}
}
