/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;

/**
 * Preset configuration for Google OAuth provider, OpenID Connect compliant.
 * @author K. Benedyczak
 */
public class GoogleProviderProperties extends CustomProviderProperties
{

	public GoogleProviderProperties(Properties properties, String prefix) throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		properties.setProperty(prefix + PROVIDER_NAME, "Google Account");
		properties.setProperty(prefix + OPENID_CONNECT, "true");
		properties.setProperty(prefix + OPENID_DISCOVERY, 
				"https://accounts.google.com/.well-known/openid-configuration");
		return properties;
	}

}
