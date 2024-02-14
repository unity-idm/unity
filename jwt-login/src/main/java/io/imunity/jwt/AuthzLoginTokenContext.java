/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt;

import java.net.URI;

import pl.edu.icm.unity.engine.api.authn.remote.RelayedAuthnState;

class AuthzLoginTokenContext extends RelayedAuthnState
{
	public final String jwtHash;
	public final URI redirectURL;

	AuthzLoginTokenContext(String jwtHash, URI redirectURL)
	{
		this.jwtHash = jwtHash;
		this.redirectURL = redirectURL;
	}

}
