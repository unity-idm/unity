/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.DBConfirmationInfo;

public class DBIdentityParamTest extends DBTypeTestBase<DBIdentityParam>
{

	@Override
	protected String getJson()
	{
		return "{\"value\":\"test@wp.pl\",\"realm\":\"realm\",\"target\":\"target\","
				+ "\"translationProfile\":\"Profile\",\"remoteIdp\":\"remoteIdp\",\"confirmationInfo\":{\"confirmed\":true,"
				+ "\"confirmationDate\":1673688981863,\"sentRequestAmount\":1},\"metadata\":{\"1\":\"v\"},\"typeId\":\"email\"}\n";

	}

	@Override
	protected DBIdentityParam getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");
		return DBIdentityParam.builder()
				.withValue("test@wp.pl")
				.withTypeId("email")
				.withRealm("realm")
				.withRemoteIdp("remoteIdp")
				.withTarget("target")
				.withMetadata(meta)
				.withTranslationProfile("Profile")
				.withConfirmationInfo(DBConfirmationInfo.builder()
						.withSentRequestAmount(1)
						.withConfirmed(true)
						.withConfirmationDate(1673688981863L)
						.build())
				.build();
	}
}
