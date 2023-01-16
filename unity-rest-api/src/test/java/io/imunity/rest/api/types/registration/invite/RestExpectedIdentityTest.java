/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration.invite;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestExpectedIdentityTest extends RestTypeBase<RestExpectedIdentity>
{

	@Override
	protected String getJson()
	{
		return "{\"identity\":\"identity\",\"expectation\":\"MANDATORY\"}\n";
	}

	@Override
	protected RestExpectedIdentity getObject()
	{
		return RestExpectedIdentity.builder()
				.withExpectation("MANDATORY")
				.withIdentity("identity")
				.build();
	}

}
