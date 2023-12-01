/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.endpoint;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import io.imunity.rest.api.types.endpoint.RestEndpointTypeDescription;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;

public class EndpointTypeDescriptionMapperTest
		extends MapperTestBase<EndpointTypeDescription, RestEndpointTypeDescription>
{

	@Override
	protected EndpointTypeDescription getFullAPIObject()
	{
		EndpointTypeDescription endpointTypeDescription = new EndpointTypeDescription("name", "desc", "binding",
				Map.of("p1k", "p1v"));

		Properties prop = new Properties();
		prop.put("key", "val");
		endpointTypeDescription.setFeatures(prop);
		return endpointTypeDescription;
	}

	@Override
	protected RestEndpointTypeDescription getFullRestObject()
	{
		return RestEndpointTypeDescription.builder()
				.withName("name")
				.withDescription("desc")
				.withFeatures(Map.of("key", "val"))
				.withPaths(Map.of("p1k", "p1v"))
				.withSupportedBinding("binding")
				.build();
	}

	@Override
	protected Pair<Function<EndpointTypeDescription, RestEndpointTypeDescription>, Function<RestEndpointTypeDescription, EndpointTypeDescription>> getMapper()
	{
		return Pair.of(EndpointTypeDescriptionMapper::map, EndpointTypeDescriptionMapper::map);
	}

}
