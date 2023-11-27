/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.endpoint;

import java.util.Optional;

import io.imunity.rest.api.types.endpoint.RestResolvedEndpoint;
import pl.edu.icm.unity.rest.mappers.authn.AuthenticationRealmMapper;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

public class ResolvedEndpointMapper
{
	public static RestResolvedEndpoint map(ResolvedEndpoint resolvedEndpoint)
	{
		return RestResolvedEndpoint.builder()
				.withEndpoint(Optional.ofNullable(resolvedEndpoint.getEndpoint())
						.map(EndpointMapper::map)
						.orElse(null))
				.withRealm(Optional.ofNullable(resolvedEndpoint.getRealm())
						.map(AuthenticationRealmMapper::map)
						.orElse(null))
				.withType(Optional.ofNullable(resolvedEndpoint.getType())
						.map(EndpointTypeDescriptionMapper::map)
						.orElse(null))
				.build();
	}

	public static ResolvedEndpoint map(RestResolvedEndpoint resolvedEndpoint)
	{
		return new ResolvedEndpoint(Optional.ofNullable(resolvedEndpoint.endpoint)
				.map(EndpointMapper::map)
				.orElse(null),
				Optional.ofNullable(resolvedEndpoint.realm)
						.map(AuthenticationRealmMapper::map)
						.orElse(null),
				Optional.ofNullable(resolvedEndpoint.type)
						.map(EndpointTypeDescriptionMapper::map)
						.orElse(null));
	}
}
