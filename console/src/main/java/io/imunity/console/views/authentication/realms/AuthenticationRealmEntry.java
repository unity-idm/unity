/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.realms;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;

import java.util.Collections;
import java.util.List;

class AuthenticationRealmEntry
{
	private static final int DEFAULT_ALLOW_FOR_REMEMBER_ME_DAYS = 14;
	private static final int DEFAULT_BLOCK_FOR = 60;
	private static final int DEFAULT_MAX_INACTIVITY = 1800;
	private static final int DEFAULT_BLOCK_AFTER_UNSUCCESSFUL_LOGINS = 5;

	final AuthenticationRealm realm;
	final List<String> endpoints;

	public AuthenticationRealmEntry()
	{
		this.realm = new AuthenticationRealm();
		realm.setAllowForRememberMeDays(DEFAULT_ALLOW_FOR_REMEMBER_ME_DAYS);
		realm.setBlockFor(DEFAULT_BLOCK_FOR);
		realm.setMaxInactivity(DEFAULT_MAX_INACTIVITY);
		realm.setBlockAfterUnsuccessfulLogins(DEFAULT_BLOCK_AFTER_UNSUCCESSFUL_LOGINS);
		realm.setRememberMePolicy(RememberMePolicy.allowFor2ndFactor);
		this.endpoints = List.of();
	}

	AuthenticationRealmEntry(AuthenticationRealm realm, List<String> endpoints)
	{
		this.realm = realm;
		this.endpoints = Collections.unmodifiableList(endpoints == null ? Collections.emptyList() : endpoints);
	}
}
