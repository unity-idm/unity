/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Date;
import java.util.List;

public class RestAttributeExtTest extends RestTypeBase<RestAttributeExt>
{

	@Override
	protected String getJson()
	{
		return "{\"remoteIdp\":\"remoteIdp\",\"translationProfile\":\"translationProfile\",\"values\":[\"v1\",\"v2\"],\"creationTs\":100,\"updateTs\":"
				+ "1000,\"direct\":true,\"name\":\"attr\",\"groupPath\":\"/A\",\"valueSyntax\":\"syntax\"}\n";

	}

	@Override
	protected RestAttributeExt getObject()
	{
		return RestAttributeExt.builder()
				.withRemoteIdp("remoteIdp")
				.withTranslationProfile("translationProfile")
				.withGroupPath("/A")
				.withValueSyntax("syntax")
				.withValues(List.of("v1", "v2"))
				.withName("attr")
				.withCreationTs(new Date(100L))
				.withUpdateTs(new Date(1000L))
				.withDirect(true)
				.build();
	}
}
