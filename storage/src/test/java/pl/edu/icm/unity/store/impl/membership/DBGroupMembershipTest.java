/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.membership;

import java.util.Date;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBGroupMembershipTest extends DBTypeTestBase<DBGroupMembership>
{

	@Override
	protected String getJson()
	{
		return "{\"remoteIdp\":\"remoteIdp\",\"translationProfile\":\"profile\",\"creationTs\":1,\"group\":\"/\",\"entityId\":1}";
	}

	@Override
	protected DBGroupMembership getObject()
	{
		return DBGroupMembership.builder()
				.withCreationTs(new Date(1))
				.withEntityId(1)
				.withGroup("/")
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("profile")
				.build();
	}

}
