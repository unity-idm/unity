/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.endpoint;

import java.util.Optional;

import io.imunity.rest.api.types.endpoint.RestEndpointConfiguration;
import io.imunity.rest.mappers.I18nStringMapper;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;

public class EndpointConfigurationMapper
{
	public static RestEndpointConfiguration map(EndpointConfiguration endpointConfiguration)
	{
		return RestEndpointConfiguration.builder()
				.withDisplayedName(Optional.ofNullable(endpointConfiguration.getDisplayedName())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withAuthenticationOptions(endpointConfiguration.getAuthenticationOptions())
				.withConfiguration(endpointConfiguration.getConfiguration())
				.withDescription(endpointConfiguration.getDescription())
				.withRealm(endpointConfiguration.getRealm())
				.withTag(endpointConfiguration.getTag())
				.build();
	}

	public static EndpointConfiguration map(RestEndpointConfiguration restEndpointConfiguration)
	{
		return new EndpointConfiguration(Optional.ofNullable(restEndpointConfiguration.displayedName)
				.map(I18nStringMapper::map)
				.orElse(null), restEndpointConfiguration.description, restEndpointConfiguration.authenticationOptions,
				restEndpointConfiguration.configuration, restEndpointConfiguration.realm,
				restEndpointConfiguration.tag);
	}
}
