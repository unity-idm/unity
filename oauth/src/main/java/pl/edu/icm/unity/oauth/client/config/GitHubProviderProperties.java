/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;

/**
 * Preset configuration for Facebook OAuth provider.
 * @author K. Benedyczak
 */
public class GitHubProviderProperties extends CustomProviderProperties
{

	public GitHubProviderProperties(Properties properties, String prefix) throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		properties.setProperty(prefix + PROVIDER_NAME, "GitHub");
		properties.setProperty(prefix + PROVIDER_LOCATION, "https://github.com/login/oauth/authorize");
		properties.setProperty(prefix + CLIENT_AUTHN_MODE, "secretPost");
		properties.setProperty(prefix + ACCESS_TOKEN_ENDPOINT, "https://github.com/login/oauth/access_token");
		properties.setProperty(prefix + PROFILE_ENDPOINT, "https://api.github.com/user");
		properties.setProperty(prefix + SCOPES, "user:email");
		return properties;
	}

}
