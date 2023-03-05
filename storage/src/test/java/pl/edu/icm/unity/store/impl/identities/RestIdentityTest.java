/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identities;

import java.util.Date;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.DBConfirmationInfo;

public class RestIdentityTest extends DBTypeTestBase<DBIdentity>
{

	@Override
	protected String getJson()
	{
		return "{\"value\":\"test@wp.pl\",\"realm\":\"realm\",\"target\":\"target\",\"translationProfile\":\"Profile\","
				+ "\"remoteIdp\":\"remoteIdp\",\"confirmationInfo\":{\"confirmed\":true,\"confirmationDate\":1667463083208,\"sentRequestAmount\":1},"
				+ "\"metadata\":{\"1\":\"v\"},\"comparableValue\":\"test@wp.pl\",\"creationTs\":1667463083208,\"updateTs\":1667463083208,\"typeId\":\"email\","
				+ "\"entityId\":1}\n";

	}

	@Override
	protected DBIdentity getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");
		return DBIdentity.builder()
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
				.withConfirmationInfo(DBConfirmationInfo.builder()
						.withSentRequestAmount(1)
						.withConfirmed(true)
						.withConfirmationDate(1667463083208L)
						.build())
				.build();
	}
}
