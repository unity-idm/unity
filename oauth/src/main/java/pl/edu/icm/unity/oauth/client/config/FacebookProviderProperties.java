/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Properties;

import pl.edu.icm.unity.oauth.client.UserProfileFetcher.ClientAuthnMode;
import pl.edu.icm.unity.server.api.PKIManagement;
import eu.unicore.util.configuration.ConfigurationException;

/**
 * Preset configuration for Facebook OAuth provider.
 * @author K. Benedyczak
 */
public class FacebookProviderProperties extends CustomProviderProperties
{

	public FacebookProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		properties.setProperty(prefix + PROVIDER_NAME, "Facebook");
		properties.setProperty(prefix + PROVIDER_LOCATION, "https://www.facebook.com/dialog/oauth");
		properties.setProperty(prefix + CLIENT_AUTHN_MODE, ClientAuthnMode.secretPost.toString());
		properties.setProperty(prefix + ACCESS_TOKEN_ENDPOINT, "https://graph.facebook.com/oauth/access_token");
		properties.setProperty(prefix + PROFILE_ENDPOINT, "https://graph.facebook.com/me/");
		properties.setProperty(prefix + SCOPES, "email");
		properties.setProperty(prefix + ACCESS_TOKEN_FORMAT, "httpParams");
		return properties;
	}

}
