/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.endpoint;

import java.util.Optional;

import io.imunity.rest.api.types.endpoint.RestEndpoint;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.Endpoint.EndpointState;

public class EndpointMapper
{
	public static RestEndpoint map(Endpoint endpoint)
	{
		return RestEndpoint.builder()
				.withName(endpoint.getName())
				.withContextAddress(endpoint.getContextAddress())
				.withRevision(endpoint.getRevision())
				.withStatus(endpoint.getState()
						.name())
				.withTypeId(endpoint.getTypeId())
				.withConfiguration(Optional.ofNullable(endpoint.getConfiguration())
						.map(EndpointConfigurationMapper::map)
						.orElse(null))
				.build();
	}

	public static Endpoint map(RestEndpoint restEndpoint)
	{
		return new Endpoint(restEndpoint.name, restEndpoint.typeId, restEndpoint.contextAddress,
				Optional.ofNullable(restEndpoint.configuration)
						.map(EndpointConfigurationMapper::map)
						.orElse(null),
				restEndpoint.revision, Optional.ofNullable(restEndpoint.status)
						.map(EndpointState::valueOf)
						.orElse(EndpointState.DEPLOYED));

	}
}
