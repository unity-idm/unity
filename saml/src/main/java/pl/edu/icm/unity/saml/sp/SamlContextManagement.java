/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.time.Duration;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthenticationContextManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Singleton component managing SAML contexts used in all remote authentications currently handled by the server.
 * See {@link RemoteAuthenticationContextManagement}.
 * @author K. Benedyczak
 */
@Component
public class SamlContextManagement extends RemoteAuthenticationContextManagement<RemoteAuthnContext>
{
	public SamlContextManagement(UnityServerConfiguration config)
	{
		super(Duration.ofSeconds(config.getIntValue(UnityServerConfiguration.MAX_REMOTE_AUTHN_TIME_S)));
	}
}
