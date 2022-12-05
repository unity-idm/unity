/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.List;

public class RestAttributeTest extends RestTypeBase<RestAttribute>
{

	@Override
	protected String getJson()
	{
		return "{\"remoteIdp\":\"remIdP\",\"translationProfile\":\"profile\",\"values\":[\"v1\",\"v2\"],\"name\":\"attr\",\"groupPath\":\"/\",\"valueSyntax\":\"string\"}";
	}

	@Override
	protected RestAttribute getObject()
	{
		return RestAttribute.builder()
				.withValueSyntax("string")
				.withName("attr")
				.withGroupPath("/")
				.withRemoteIdp("remIdP")
				.withTranslationProfile("profile")
				.withValues(List.of("v1", "v2"))
				.build();
	}

}
