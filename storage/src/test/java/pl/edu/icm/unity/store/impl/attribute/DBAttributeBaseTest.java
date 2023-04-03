/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAttributeBaseTest extends DBTypeTestBase<DBAttribute>
{

	@Override
	protected String getJson()
	{
		return "{\"remoteIdp\":\"remIdP\",\"translationProfile\":\"profile\",\"values\":[\"v1\",\"v2\"],\"name\":\"attr\",\"groupPath\":\"/\","
				+ "\"valueSyntax\":\"string\"}";
	}

	@Override
	protected DBAttribute getObject()
	{
		return DBAttribute.builder()
				.withValueSyntax("string")
				.withName("attr")
				.withGroupPath("/")
				.withRemoteIdp("remIdP")
				.withTranslationProfile("profile")
				.withValues(List.of("v1", "v2"))
				.build();
	}

}
