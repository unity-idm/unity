/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.endpoint;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.endpoint.RestEndpoint;
import io.imunity.rest.api.types.endpoint.RestEndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.restadm.mappers.MapperWithMinimalTestBase;

public class EndpointMapperTest extends MapperWithMinimalTestBase<Endpoint, RestEndpoint>
{

	@Override
	protected Endpoint getFullAPIObject()
	{
		return new Endpoint("endpoint", "rest", "/rest",
				new EndpointConfiguration(new I18nString("disp"), "desc", List.of("ao1"), "conf", "realm", "tag1"), 1);
	}

	@Override
	protected RestEndpoint getFullRestObject()
	{
		return RestEndpoint.builder()
				.withConfiguration(RestEndpointConfiguration.builder()
						.withDisplayedName(RestI18nString.builder()
								.withDefaultValue("disp")
								.build())
						.withDescription("desc")
						.withRealm("realm")
						.withTag("tag1")
						.withAuthenticationOptions(List.of("ao1"))
						.withConfiguration("conf")
						.build())
				.withName("endpoint")
				.withTypeId("rest")
				.withRevision(1)
				.withStatus("DEPLOYED")
				.withContextAddress("/rest")
				.build();
	}

	@Override
	protected Endpoint getMinAPIObject()
	{

		return new Endpoint("endpoint", null, null, null, 0);
	}

	@Override
	protected RestEndpoint getMinRestObject()
	{
		return RestEndpoint.builder()
				.withName("endpoint")
				.withRevision(0)
				.build();
	}

	@Override
	protected Pair<Function<Endpoint, RestEndpoint>, Function<RestEndpoint, Endpoint>> getMapper()
	{
		return Pair.of(EndpointMapper::map, EndpointMapper::map);
	}
}
