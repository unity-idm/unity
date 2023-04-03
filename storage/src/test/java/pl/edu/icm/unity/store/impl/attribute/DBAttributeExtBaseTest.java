/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.attribute;

import java.util.Date;
import java.util.List;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAttributeExtBaseTest extends DBTypeTestBase<DBAttributeExt>
{

	@Override
	protected String getJson()
	{
		return "{\"remoteIdp\":\"remoteIdp\",\"translationProfile\":\"translationProfile\",\"values\":[\"v1\",\"v2\"],\"creationTs\":100,\"updateTs\":"
				+ "1000,\"direct\":true,\"name\":\"attr\",\"groupPath\":\"/A\",\"valueSyntax\":\"syntax\"}\n";

	}

	@Override
	protected DBAttributeExt getObject()
	{
		return DBAttributeExt.builder()
				.withCreationTs(new Date(100L))
				.withUpdateTs(new Date(1000L))
				.withDirect(true)
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("translationProfile")
				.withGroupPath("/A")
				.withValueSyntax("syntax")
				.withValues(List.of("v1", "v2"))
				.withName("attr")
				.build();
	}
}
