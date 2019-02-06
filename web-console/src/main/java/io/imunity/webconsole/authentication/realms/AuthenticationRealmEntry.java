/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * 
 * @author P.Piernik
 *
 */
class AuthenticationRealmEntry
{
	final AuthenticationRealm realm;
	final List<String> endpoints;

	AuthenticationRealmEntry(AuthenticationRealm realm, List<String> endpoints)
	{
		this.realm = realm;
		this.endpoints = Collections.unmodifiableList(endpoints == null ? Collections.emptyList() : endpoints);
	}
}
