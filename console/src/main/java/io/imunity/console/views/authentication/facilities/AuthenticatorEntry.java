/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication.facilities;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;

import java.util.Collections;
import java.util.List;


record AuthenticatorEntry(
		AuthenticatorDefinition authenticator,
		List<String> endpoints)
{
	AuthenticatorEntry(AuthenticatorDefinition authenticator, List<String> endpoints)
	{
		this.authenticator = authenticator;
		this.endpoints = Collections.unmodifiableList(endpoints == null ? Collections.emptyList() : endpoints);
	}
}
