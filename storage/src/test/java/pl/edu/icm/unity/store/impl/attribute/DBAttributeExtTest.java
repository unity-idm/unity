/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAttributeExtTest extends DBTypeTestBase<DBAttributeExtBase>
{

	@Override
	protected String getJson()
	{
		return "{\"remoteIdp\":\"remoteIdp\",\"translationProfile\":\"translationProfile\",\"values\":[\"v1\",\"v2\"],\"creationTs\":100,\"updateTs\":"
				+ "1000,\"direct\":true}\n";

	}

	@Override
	protected DBAttributeExtBase getObject()
	{
		return DBAttributeExtBase.builder()
				.withCreationTs(new Date(100L))
				.withUpdateTs(new Date(1000L))
				.withDirect(true)
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("translationProfile")
				.withValues(List.of("v1", "v2"))
				.build();
	}
}
