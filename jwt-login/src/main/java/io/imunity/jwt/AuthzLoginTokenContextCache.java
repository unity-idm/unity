/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt;

import java.time.Duration;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthenticationContextManagement;

@Component
class AuthzLoginTokenContextCache extends RemoteAuthenticationContextManagement<AuthzLoginTokenContext>
{
	public AuthzLoginTokenContextCache()
	{
		super(Duration.ofMinutes(2));
	}
}
