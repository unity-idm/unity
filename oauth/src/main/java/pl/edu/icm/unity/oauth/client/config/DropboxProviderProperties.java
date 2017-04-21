/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.server.api.PKIManagement;

/**
 * Preset configuration for Facebook OAuth provider.
 * @author K. Benedyczak
 */
public class DropboxProviderProperties extends CustomProviderProperties
{

	public DropboxProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "Dropbox");
		setIfUnset(properties, prefix + PROVIDER_LOCATION, "https://www.dropbox.com/1/oauth2/authorize");
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE, ClientAuthnMode.secretBasic.toString());
		setIfUnset(properties, prefix + ACCESS_TOKEN_ENDPOINT, "https://api.dropbox.com/1/oauth2/token");
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://api.dropbox.com/1/account/info");
		setIfUnset(properties, prefix + ICON_URL, "file:../common/img/external/dropbox-small.png");
		return properties;
	}

}
