/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.ecp;

import java.time.Duration;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.authn.remote.RemoteAuthenticationContextManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Singleton component managing SAML ECP contexts used in all remote authentications currently handled by the server.
 * See {@link RemoteAuthenticationContextManagement}.
 * @author K. Benedyczak
 */
@Component
public class ECPContextManagement extends RemoteAuthenticationContextManagement<ECPAuthnState>
{
	public ECPContextManagement(UnityServerConfiguration config)
	{
		super(Duration.ofSeconds(config.getIntValue(UnityServerConfiguration.MAX_REMOTE_AUTHN_TIME_S)));
	}
}
