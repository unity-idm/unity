/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identities;

import java.util.Date;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.DBTypeTestBase;
import pl.edu.icm.unity.store.types.common.DBConfirmationInfo;

public class RestIdentityBaseTest extends DBTypeTestBase<DBIdentityBase>
{

	@Override
	protected String getJson()
	{
		return "{\"value\":\"test@wp.pl\",\"realm\":\"realm\",\"target\":\"target\",\"translationProfile\":\"Profile\","
				+ "\"remoteIdp\":\"remoteIdp\",\"confirmationInfo\":{\"confirmed\":true,\"confirmationDate\":1667463083208,\"sentRequestAmount\":1},"
				+ "\"metadata\":{\"1\":\"v\"},\"comparableValue\":\"test@wp.pl\",\"creationTs\":1667463083208,\"updateTs\":1667463083208"
				+"}\n";

	}

	@Override
	protected DBIdentityBase getObject()
	{
		ObjectNode meta = MAPPER.createObjectNode();
		meta.put("1", "v");
		return DBIdentityBase.builder()
				.withCreationTs(new Date(1667463083208L))
				.withUpdateTs(new Date(1667463083208L))
				.withComparableValue("test@wp.pl")
				.withValue("test@wp.pl")
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
