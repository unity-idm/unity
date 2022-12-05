/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;

import com.fasterxml.jackson.databind.node.ObjectNode;

import io.imunity.rest.api.types.confirmation.RestConfirmationInfo;

public class RestIdentityTest extends RestTypeBase<RestIdentity>
{

	@Override
	protected String getJson()
	{
		return "{\"value\":\"test@wp.pl\",\"realm\":\"realm\",\"target\":\"target\",\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\",\"confirmationInfo\":{\"confirmed\":true,\"confirmationDate\":1667463083208,\"sentRequestAmount\":1},\"metadata\":{\"1\":\"v\"},\"comparableValue\":\"test@wp.pl\",\"creationTs\":1667463083208,\"updateTs\":1667463083208,\"typeId\":\"email\",\"entityId\":1}\n";

	}

	@Override
	protected RestIdentity getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");
		return RestIdentity.builder()
				.withCreationTs(new Date(1667463083208L))
				.withUpdateTs(new Date(1667463083208L))
				.withComparableValue("test@wp.pl")
				.withValue("test@wp.pl")
				.withEntityId(1L)
				.withTypeId("email")
				.withRealm("realm")
				.withRemoteIdp("remoteIdp")
				.withTarget("target")
				.withMetadata(meta)
				.withTranslationProfile("Profile")
				.withConfirmationInfo(RestConfirmationInfo.builder()
						.withSentRequestAmount(1)
						.withConfirmed(true)
						.withConfirmationDate(1667463083208L)
						.build())
				.build();
	}
}
