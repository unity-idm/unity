/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.realm;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

public class AuthenticationRealmMapperTest extends MapperTestBase<AuthenticationRealm, DBAuthenticationRealm>
{

	@Override
	protected AuthenticationRealm getFullAPIObject()
	{
		return new AuthenticationRealm("realm", "desc", 1, 1, RememberMePolicy.allowFor2ndFactor, 1, 2);
	}

	@Override
	protected DBAuthenticationRealm getFullDBObject()
	{
		return DBAuthenticationRealm.builder()
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
	protected Pair<Function<AuthenticationRealm, DBAuthenticationRealm>, Function<DBAuthenticationRealm, AuthenticationRealm>> getMapper()
	{
		return Pair.of(AuthenticationRealmMapper::map, AuthenticationRealmMapper::map);
	}

}
