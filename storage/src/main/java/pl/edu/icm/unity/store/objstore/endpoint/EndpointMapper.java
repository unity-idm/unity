/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.endpoint;

import java.util.Optional;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.Endpoint.EndpointState;

class EndpointMapper
{
	static DBEndpoint map(Endpoint endpoint)
	{
		return DBEndpoint.builder()
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

	static Endpoint map(DBEndpoint restEndpoint)
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
