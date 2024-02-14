/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt;

import java.net.URI;

import org.springframework.stereotype.Component;

@Component
class AuthzLoginTokenService
{
	private final AuthzLoginTokenContextCache cache;
	
	AuthzLoginTokenService(AuthzLoginTokenContextCache cache)
	{
		this.cache = cache;
	}

	String getAuthzLoginToken(String jwtHash, URI redirectURL)
	{
		AuthzLoginTokenContext context = new AuthzLoginTokenContext(jwtHash, redirectURL);
		cache.addAuthnContext(context);
		return context.getRelayState();
	}
	
	AuthzLoginTokenContext getAuthzLoginTokenContext(String authzLoginToken)
	{
		return cache.getAndRemoveAuthnContext(authzLoginToken);
	}
}
