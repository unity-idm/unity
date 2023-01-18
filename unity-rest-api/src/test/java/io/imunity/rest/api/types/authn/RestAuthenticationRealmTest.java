/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.authn;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestAuthenticationRealmTest extends RestTypeBase<RestAuthenticationRealm>
{

	@Override
	protected String getJson()
	{
		return "{\"description\":\"desc\",\"name\":\"realm\",\"rememberMePolicy\":\"allowFor2ndFactor\","
				+ "\"allowForRememberMeDays\":1,\"blockAfterUnsuccessfulLogins\":1,\"blockFor\":1,\"maxInactivity\":2}";
	}

	@Override
	protected RestAuthenticationRealm getObject()
	{
		return RestAuthenticationRealm.builder()
				.withName("realm")
				.withDescription("desc")
				.withRememberMePolicy("allowFor2ndFactor")
				.withAllowForRememberMeDays(1)
				.withBlockAfterUnsuccessfulLogins(1)
				.withMaxInactivity(2)
				.withBlockFor(1)
				.build();
	}

}
