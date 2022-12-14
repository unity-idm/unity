/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.endpoint;

import java.util.List;
import java.util.Map;

import io.imunity.rest.api.types.authn.RestAuthenticationRealm;
import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestResolvedEndpointTest extends RestTypeBase<RestResolvedEndpoint>
{

	@Override
	protected String getJson()
	{
		return "{\"endpoint\":{\"name\":\"endpoint\",\"typeId\":\"rest\",\"contextAddress\":\"/rest\","
				+ "\"configuration\":{\"displayedName\":{\"DefaultValue\":\"disp\",\"Map\":{}},"
				+ "\"description\":\"desc\",\"configuration\":\"conf\",\"realm\":\"realm\",\"tag\":\"tag1\","
				+ "\"authenticationOptions\":[\"ao1\"]},\"revision\":1,\"status\":\"DEPLOYED\"},"
				+ "\"realm\":{\"description\":\"desc\",\"name\":\"realm\",\"rememberMePolicy\":\"allowFor2ndFactor\","
				+ "\"allowForRememberMeDays\":1,\"blockAfterUnsuccessfulLogins\":1,\"blockFor\":1,\"maxInactivity\":2},"
				+ "\"type\":{\"name\":\"name\",\"description\":\"desc\",\"supportedBinding\":\"binding\","
				+ "\"paths\":{\"p1k\":\"p1v\"},\"features\":{\"f1k\":\"f1v\"}}}\n";
	}

	@Override
	protected RestResolvedEndpoint getObject()
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
						.withFeatures(Map.of("f1k", "f1v"))
						.withPaths(Map.of("p1k", "p1v"))
						.withSupportedBinding("binding")
						.build())

				.build();
	}

}
