/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAttributeTest extends DBTypeTestBase<DBAttributeBase>
{

	@Override
	protected String getJson()
	{
		return "{\"remoteIdp\":\"remIdP\",\"translationProfile\":\"profile\",\"values\":[\"v1\",\"v2\"]}";
	}

	@Override
	protected DBAttributeBase getObject()
	{
		return DBAttributeBase.builder()
				.withRemoteIdp("remIdP")
				.withTranslationProfile("profile")
				.withValues(List.of("v1", "v2"))
				.build();
	}

}
