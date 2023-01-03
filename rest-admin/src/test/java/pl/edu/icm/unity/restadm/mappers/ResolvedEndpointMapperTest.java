/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.authn.RestAuthenticationRealm;
import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.endpoint.RestEndpoint;
import io.imunity.rest.api.types.endpoint.RestEndpointConfiguration;
import io.imunity.rest.api.types.endpoint.RestEndpointTypeDescription;
import io.imunity.rest.api.types.endpoint.RestResolvedEndpoint;
import pl.edu.icm.unity.restadm.mappers.endpoint.ResolvedEndpointMapper;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

public class ResolvedEndpointMapperTest extends MapperTestBase<ResolvedEndpoint, RestResolvedEndpoint>
{

	@Override
	protected ResolvedEndpoint getAPIObject()
	{
		AuthenticationRealm authenticationRealm = new AuthenticationRealm("realm", "desc", 1, 1,
				RememberMePolicy.allowFor2ndFactor, 1, 2);
		EndpointTypeDescription endpointTypeDescription = new EndpointTypeDescription("name", "desc", "binding",
				Map.of("p1k", "p1v"));
		EndpointConfiguration endpointConfiguration = new EndpointConfiguration(new I18nString("disp"), "desc",
				List.of("ao1"), "conf", "realm", "tag1");
		Endpoint endpoint = new Endpoint("endpoint", "rest", "/rest", endpointConfiguration, 1);
		return new ResolvedEndpoint(endpoint, authenticationRealm, endpointTypeDescription);
	}

	@Override
	protected RestResolvedEndpoint getRestObject()
	{
		return RestResolvedEndpoint.builder()
				.withEndpoint(RestEndpoint.builder()
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
						.build())
				.withRealm(RestAuthenticationRealm.builder()
						.withName("realm")
						.withDescription("desc")
						.withRememberMePolicy("allowFor2ndFactor")
						.withAllowForRememberMeDays(1)
						.withBlockAfterUnsuccessfulLogins(1)
						.withMaxInactivity(2)
						.withBlockFor(1)
						.build())
				.withType(RestEndpointTypeDescription.builder()
						.withName("name")
						.withDescription("desc")
						.withPaths(Map.of("p1k", "p1v"))
						.withSupportedBinding("binding")
						.build())

				.build();
	}

	@Override
	protected Pair<Function<ResolvedEndpoint, RestResolvedEndpoint>, Function<RestResolvedEndpoint, ResolvedEndpoint>> getMapper()
	{
		return Pair.of(ResolvedEndpointMapper::map, ResolvedEndpointMapper::map);
	}

}
