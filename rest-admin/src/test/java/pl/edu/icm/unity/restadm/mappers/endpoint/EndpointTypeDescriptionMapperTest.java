/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.endpoint;

import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.endpoint.RestEndpointTypeDescription;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;

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
