/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.realm;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;

class AuthenticationRealmMapper
{
	static DBAuthenticationRealm map(AuthenticationRealm realm)
	{
		return DBAuthenticationRealm.builder()
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

	static AuthenticationRealm map(DBAuthenticationRealm dbAuthenticationRealm)
	{
		return new AuthenticationRealm(dbAuthenticationRealm.name, dbAuthenticationRealm.description,
				dbAuthenticationRealm.blockAfterUnsuccessfulLogins, dbAuthenticationRealm.blockFor,
				RememberMePolicy.valueOf(dbAuthenticationRealm.rememberMePolicy),
				dbAuthenticationRealm.allowForRememberMeDays, dbAuthenticationRealm.maxInactivity);
	}
}
