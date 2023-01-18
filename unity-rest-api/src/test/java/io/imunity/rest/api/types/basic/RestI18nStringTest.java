/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.basic;

import java.util.Map;

public class RestI18nStringTest extends RestTypeBase<RestI18nString>
{

	@Override
	protected String getJson()
	{
		return "{\"DefaultValue\":\"default\",\"Map\":{\"pl\":\"plval\",\"en\":\"enval\"}}";
	}

	@Override
	protected RestI18nString getObject()
	{
		return RestI18nString.builder()
				.withDefaultValue("default")
				.withValues(Map.of("pl", "plval", "en", "enval"))
				.build();
	}

}
