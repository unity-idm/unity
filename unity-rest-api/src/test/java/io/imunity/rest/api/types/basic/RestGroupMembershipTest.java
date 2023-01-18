/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;

public class RestGroupMembershipTest extends RestTypeBase<RestGroupMembership>
{

	@Override
	protected String getJson()
	{
		return "{\"remoteIdp\":\"remoteIdp\",\"translationProfile\":\"profile\",\"creationTs\":1,\"group\":\"/\",\"entityId\":1}";
	}

	@Override
	protected RestGroupMembership getObject()
	{
		return RestGroupMembership.builder()
				.withCreationTs(new Date(1))
				.withEntityId(1)
				.withGroup("/")
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("profile")
				.build();
	}

}
