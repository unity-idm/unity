/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Properties;

import pl.edu.icm.unity.server.api.PKIManagement;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Preset configuration for Microsoft OAuth provider.
 * @author K. Benedyczak
 */
public class MicrosoftProviderProperties extends CustomProviderProperties
{

	public MicrosoftProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		properties.setProperty(prefix + PROVIDER_NAME, "Microsoft Live");
		properties.setProperty(prefix + PROVIDER_LOCATION, "https://login.live.com/oauth20_authorize.srf");
		properties.setProperty(prefix + CLIENT_AUTHN_MODE, ClientAuthnMode.secretPost.toString());
		properties.setProperty(prefix + ACCESS_TOKEN_ENDPOINT, "https://login.live.com/oauth20_token.srf");
		properties.setProperty(prefix + PROFILE_ENDPOINT, "https://apis.live.net/v5.0/me");
		properties.setProperty(prefix + SCOPES, "wl.basic");
		return properties;
	}

}
