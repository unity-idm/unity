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
public class FacebookProviderProperties extends CustomProviderProperties
{

	public FacebookProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "Facebook");
		setIfUnset(properties, prefix + PROVIDER_LOCATION, "https://www.facebook.com/dialog/oauth");
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE, ClientAuthnMode.secretPost.toString());
		setIfUnset(properties, prefix + ACCESS_TOKEN_ENDPOINT, "https://graph.facebook.com/oauth/access_token");
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://graph.facebook.com/me/");
		setIfUnset(properties, prefix + SCOPES, "email");
		setIfUnset(properties, prefix + ACCESS_TOKEN_FORMAT, AccessTokenFormat.standard.toString());
		setIfUnset(properties, prefix + ICON_URL, "file:../common/img/external/FB-small.png");
		return properties;
	}

}
