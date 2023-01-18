/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.endpoint;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.endpoint.RestEndpointConfiguration;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;

public class EndpointConfigurationMapperTest extends MapperTestBase<EndpointConfiguration, RestEndpointConfiguration>
{

	@Override
	protected EndpointConfiguration getAPIObject()
	{
		return new EndpointConfiguration(new I18nString("disp"), "desc", List.of("ao1"), "conf", "realm", "tag1");
	}

	@Override
	protected RestEndpointConfiguration getRestObject()
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
	protected Pair<Function<EndpointConfiguration, RestEndpointConfiguration>, Function<RestEndpointConfiguration, EndpointConfiguration>> getMapper()
	{
		return Pair.of(EndpointConfigurationMapper::map, EndpointConfigurationMapper::map);
	}

}
