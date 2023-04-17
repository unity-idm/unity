/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;

import java.util.Properties;

/**
 * Preset configuration for Google OAuth provider, OpenID Connect compliant.
 * @author K. Benedyczak
 */
public class GoogleProviderProperties extends CustomProviderProperties
{

	public GoogleProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "Google");
		setIfUnset(properties, prefix + OPENID_CONNECT, "true");
		setIfUnset(properties, prefix + OPENID_DISCOVERY, 
				"https://accounts.google.com/.well-known/openid-configuration");
		setIfUnset(properties, prefix + SCOPES, "openid profile email");
		setIfUnset(properties, prefix + ICON_URL, "../unitygw/img/external/google-small.png");
		setDefaultProfileIfUnset(properties, prefix, "sys:oidc");
		return properties;
	}

}
