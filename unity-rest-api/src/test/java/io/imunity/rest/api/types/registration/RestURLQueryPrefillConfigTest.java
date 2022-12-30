/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestURLQueryPrefillConfigTest extends RestTypeBase<RestURLQueryPrefillConfig>
{

	@Override
	protected String getJson()
	{
		return "{\"paramName\":\"param\",\"mode\":\"DEFAULT\"}\n";
	}

	@Override
	protected RestURLQueryPrefillConfig getObject()
	{
		return RestURLQueryPrefillConfig.builder()
				.withMode("DEFAULT")
				.withParamName("param")
				.build();
	}

}
