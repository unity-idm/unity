/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestRegistrationContextTest extends RestTypeBase<RestRegistrationContext>
{

	@Override
	protected String getJson()
	{
		return "{\"isOnIdpEndpoint\":true,\"triggeringMode\":\"manualAtLogin\"}\n";
	}

	@Override
	protected RestRegistrationContext getObject()
	{
		return RestRegistrationContext.builder()
				.withIsOnIdpEndpoint(true)
				.withTriggeringMode("manualAtLogin")
				.build();
	}

}
