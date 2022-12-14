/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.endpoint;

import java.util.Map;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestEndpointTypeDescriptionTest extends RestTypeBase<RestEndpointTypeDescription>
{

	@Override
	protected String getJson()
	{
		return "{\"name\":\"name\",\"description\":\"desc\",\"supportedBinding\":\"binding\",\"paths\":{\"p1k\":\"p1v\"},\"features\":{\"key\":\"val\"}}\n";
	}

	@Override
	protected RestEndpointTypeDescription getObject()
	{
		return RestEndpointTypeDescription.builder()
				.withName("name")
				.withDescription("desc")
				.withFeatures(Map.of("key", "val"))
				.withPaths(Map.of("p1k", "p1v"))
				.withSupportedBinding("binding")
				.build();
	}

}
