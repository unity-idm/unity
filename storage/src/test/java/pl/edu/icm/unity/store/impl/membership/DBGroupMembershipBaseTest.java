/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.membership;

import java.util.Date;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBGroupMembershipBaseTest extends DBTypeTestBase<DBGroupMembershipBase>
{

	@Override
	protected String getJson()
	{
		return "{\"remoteIdp\":\"remoteIdp\",\"translationProfile\":\"profile\",\"creationTs\":1}";
	}

	@Override
	protected DBGroupMembershipBase getObject()
	{
		return DBGroupMembershipBase.builder()
				.withCreationTs(new Date(1))				
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("profile")
				.build();
	}

}
