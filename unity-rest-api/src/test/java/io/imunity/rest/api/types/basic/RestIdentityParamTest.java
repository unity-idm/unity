/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;

public class RestIdentityParamTest extends RestTypeBase<RestIdentityParam>
{

	@Override
	protected String getJson()
	{
		return "{\"value\":\"test@wp.pl\",\"realm\":\"realm\",\"target\":\"target\","
				+ "\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\",\"confirmationInfo\":{\"confirmed\":true,"
				+ "\"confirmationDate\":1673688981863,\"sentRequestAmount\":1},\"metadata\":{\"1\":\"v\"},\"typeId\":\"email\"}\n";

	}

	@Override
	protected RestIdentityParam getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");
		return RestIdentityParam.builder()
				.withValue("test@wp.pl")
				.withTypeId("email")
				.withRealm("realm")
				.withRemoteIdp("remoteIdp")
				.withTarget("target")
				.withMetadata(meta)
				.withTranslationProfile("Profile")
				.withConfirmationInfo(RestConfirmationInfo.builder()
						.withSentRequestAmount(1)
						.withConfirmed(true)
						.withConfirmationDate(1673688981863L)
						.build())
				.build();
	}
}
