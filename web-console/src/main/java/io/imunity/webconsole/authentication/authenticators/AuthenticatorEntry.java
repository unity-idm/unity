/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.authenticators;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorDefinition;

/**
 * Data object behind a row in {@link AuthenticatorsComponent}. Stores
 * authenticator information
 * 
 * @author P.Piernik
 *
 */
class AuthenticatorEntry
{
	final AuthenticatorDefinition authenticator;
	final List<String> endpoints;

	AuthenticatorEntry(AuthenticatorDefinition authneticator, List<String> endpoints)
	{
		this.authenticator = authneticator;
		this.endpoints = Collections.unmodifiableList(endpoints == null ? Collections.emptyList() : endpoints);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.authenticator, this.endpoints);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final AuthenticatorEntry other = (AuthenticatorEntry) obj;
		return Objects.equals(this.authenticator, other.authenticator)
				&& Objects.equals(this.endpoints, other.endpoints);
	}
}
