/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;

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
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://graph.facebook.com/me?fields="
				+ "id,name,address,birthday,currency,email,first_name,gender,"
				+ "last_name,locale,middle_name,public_key,timezone,website");
		setIfUnset(properties, prefix + SCOPES, "email public_profile");
		setIfUnset(properties, prefix + ACCESS_TOKEN_FORMAT, AccessTokenFormat.standard.toString());
		setIfUnset(properties, prefix + ICON_URL, "file:../common/img/external/FB-small.png");
		setIfUnset(properties, prefix + CommonWebAuthnProperties.TRANSLATION_PROFILE, "sys:facebook");
		return properties;
	}

}
