/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.time.Duration;

import org.springframework.stereotype.Component;

@Component
public class SharedRemoteAuthenticationContextStore extends RemoteAuthenticationContextManagement<RedirectedAuthnState>
{
	public SharedRemoteAuthenticationContextStore()
	{
		super(Duration.ofMinutes(15));
	}
}
