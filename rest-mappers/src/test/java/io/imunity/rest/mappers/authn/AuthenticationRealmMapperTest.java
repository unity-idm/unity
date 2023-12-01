/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.authn;

import java.util.function.Function;

import io.imunity.rest.api.types.authn.RestAuthenticationRealm;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;

public class AuthenticationRealmMapperTest extends MapperTestBase<AuthenticationRealm, RestAuthenticationRealm>
{

	@Override
	protected AuthenticationRealm getFullAPIObject()
	{
		return new AuthenticationRealm("realm", "desc", 1, 1, RememberMePolicy.allowFor2ndFactor, 1, 2);
	}

	@Override
	protected RestAuthenticationRealm getFullRestObject()
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

	@Override
	protected Pair<Function<AuthenticationRealm, RestAuthenticationRealm>, Function<RestAuthenticationRealm, AuthenticationRealm>> getMapper()
	{
		return Pair.of(AuthenticationRealmMapper::map, AuthenticationRealmMapper::map);
	}

}
