/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

public class RestGroupPropertyTest extends RestTypeBase<RestGroupProperty>
{

	@Override
	protected String getJson()
	{
		return "{\"key\":\"key\",\"value\":\"val\"}";

	}

	@Override
	protected RestGroupProperty getObject()
	{
		return RestGroupProperty.builder()
				.withKey("key")
				.withValue("val")
				.build();
	}

}
