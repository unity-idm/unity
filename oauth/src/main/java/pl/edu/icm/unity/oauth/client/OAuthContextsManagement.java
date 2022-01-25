/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.time.Duration;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthenticationContextManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Responsible for management of OAuth authentication contexts.
 * See {@link RemoteAuthenticationContextManagement}.
 * @author K. Benedyczak
 */
@Component
public class OAuthContextsManagement extends RemoteAuthenticationContextManagement<OAuthContext>
{
	public OAuthContextsManagement(UnityServerConfiguration config)
	{
		super(Duration.ofSeconds(config.getIntValue(UnityServerConfiguration.MAX_REMOTE_AUTHN_TIME_S)));
	}
}
