/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.endpoint;

import java.util.Optional;

import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.store.types.common.I18nStringMapper;

class EndpointConfigurationMapper
{
	static DBEndpointConfiguration map(EndpointConfiguration endpointConfiguration)
	{
		return DBEndpointConfiguration.builder()
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

	static EndpointConfiguration map(DBEndpointConfiguration restEndpointConfiguration)
	{
		return new EndpointConfiguration(Optional.ofNullable(restEndpointConfiguration.displayedName)
				.map(I18nStringMapper::map)
				.orElse(null), restEndpointConfiguration.description, restEndpointConfiguration.authenticationOptions,
				restEndpointConfiguration.configuration, restEndpointConfiguration.realm,
				restEndpointConfiguration.tag);
	}
}
