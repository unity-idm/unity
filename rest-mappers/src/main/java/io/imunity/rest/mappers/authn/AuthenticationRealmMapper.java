/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.authn;

import io.imunity.rest.api.types.authn.RestAuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;

public class AuthenticationRealmMapper
{
	public static RestAuthenticationRealm map(AuthenticationRealm realm)
	{
		return RestAuthenticationRealm.builder()
				.withName(realm.getName())
				.withDescription(realm.getDescription())
				.withRememberMePolicy(realm.getRememberMePolicy()
						.name())
				.withAllowForRememberMeDays(realm.getAllowForRememberMeDays())
				.withBlockAfterUnsuccessfulLogins(realm.getBlockAfterUnsuccessfulLogins())
				.withMaxInactivity(realm.getMaxInactivity())
				.withBlockFor(realm.getBlockFor())
				.build();

	}

	public static AuthenticationRealm map(RestAuthenticationRealm restAuthenticationRealm)
	{
		return new AuthenticationRealm(restAuthenticationRealm.name, restAuthenticationRealm.description,
				restAuthenticationRealm.blockAfterUnsuccessfulLogins, restAuthenticationRealm.blockFor,
				RememberMePolicy.valueOf(restAuthenticationRealm.rememberMePolicy),
				restAuthenticationRealm.allowForRememberMeDays, restAuthenticationRealm.maxInactivity);
	}
}
