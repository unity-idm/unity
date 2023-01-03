/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.endpoint;

import java.util.List;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestEndpointConfigurationTest extends RestTypeBase<RestEndpointConfiguration>
{

	@Override
	protected String getJson()
	{
		return "{\"displayedName\":{\"DefaultValue\":\"disp\",\"Map\":{}},\"description\":\"desc\",\"configuration\":\"conf\","
				+ "\"realm\":\"realm\",\"tag\":\"tag1\",\"authenticationOptions\":[\"ao1\"]}\n";
	}

	@Override
	protected RestEndpointConfiguration getObject()
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

}
