/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.endpoint;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.endpoint.RestEndpointTypeDescription;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

public class EndpointTypeDescriptionMapper
{
	public static RestEndpointTypeDescription map(EndpointTypeDescription endpointTypeDescription)
	{
		return RestEndpointTypeDescription.builder()
				.withName(endpointTypeDescription.getName())
				.withDescription(endpointTypeDescription.getDescription())
				.withPaths(endpointTypeDescription.getPaths())
				.withFeatures(Optional.ofNullable(endpointTypeDescription.getFeatures())
						.map(f -> f.entrySet()
								.stream()
								.collect(Collectors.toMap(e -> e.getKey()
										.toString(),
										e -> e.getValue()
												.toString())))
						.orElse(null))
				.withSupportedBinding(endpointTypeDescription.getSupportedBinding())
				.build();
	}

	public static EndpointTypeDescription map(RestEndpointTypeDescription restEndpointTypeDescription)
	{
		EndpointTypeDescription endpointTypeDescription = new EndpointTypeDescription(restEndpointTypeDescription.name,
				restEndpointTypeDescription.description, restEndpointTypeDescription.supportedBinding,
				restEndpointTypeDescription.paths);

		endpointTypeDescription.setFeatures(Optional.ofNullable(restEndpointTypeDescription.features)
				.map(EndpointTypeDescriptionMapper::toProperties)
				.orElse(null));
		return endpointTypeDescription;

	}
	
	private static Properties toProperties(Map<String, String> map)
	{
		Properties p = new Properties();
		p.putAll(map);
		return p;
	}
}
