/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.endpoint;

import java.util.List;
import java.util.function.Function;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.endpoint.RestEndpointConfiguration;
import io.imunity.rest.mappers.MapperWithMinimalTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;

public class EndpointConfigurationMapperTest extends MapperWithMinimalTestBase<EndpointConfiguration, RestEndpointConfiguration>
{

	@Override
	protected EndpointConfiguration getFullAPIObject()
	{
		return new EndpointConfiguration(new I18nString("disp"), "desc", List.of("ao1"), "conf", "realm", "tag1");
	}

	@Override
	protected RestEndpointConfiguration getFullRestObject()
	{
		return RestEndpointConfiguration.builder()
				.withDisplayedName(RestI18nString.builder()
						.withDefaultValue("disp")
						.build())
				.withDescription("desc")
				.withRealm("realm")
				.withTag("tag1")
				.withAuthenticationOptions(List.of("ao1"))
				.withConfiguration("conf")
				.build();
	}

	@Override
	protected EndpointConfiguration getMinAPIObject()
	{
		return new EndpointConfiguration(null, null, null, null, null);
	}

	@Override
	protected RestEndpointConfiguration getMinRestObject()
	{
		return RestEndpointConfiguration.builder()		
				.withTag("")
				.build();
	}

	@Override
	protected Pair<Function<EndpointConfiguration, RestEndpointConfiguration>, Function<RestEndpointConfiguration, EndpointConfiguration>> getMapper()
	{
		return Pair.of(EndpointConfigurationMapper::map, EndpointConfigurationMapper::map);
	}
}
