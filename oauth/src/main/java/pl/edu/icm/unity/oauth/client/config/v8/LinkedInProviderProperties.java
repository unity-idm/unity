/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config.v8;

import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;

/**
 * Preset configuration for LinkedIn OAuth provider.
 * @author P.Piernik
 */
public class LinkedInProviderProperties extends CustomProviderProperties
{

	public LinkedInProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement)
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "LinkedIn");
		setIfUnset(properties, prefix + PROVIDER_LOCATION, "https://www.linkedin.com/oauth/v2/authorization");
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE, ClientAuthnMode.secretPost.toString());
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS, ClientAuthnMode.secretBasic.toString());
		setIfUnset(properties, prefix + ACCESS_TOKEN_ENDPOINT, "https://www.linkedin.com/oauth/v2/accessToken");
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://api.linkedin.com/v2/me?projection=" 
				+ "(id,localizedFirstName,localizedLastName,profilePicture(displayImage~:playableStreams),localizedHeadline,vanityName)");
		setIfUnset(properties, prefix + PROFILE_ENDPOINT+".1", 
				"https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))");
		setIfUnset(properties, prefix + SCOPES, "r_liteprofile r_basicprofile r_emailaddress");
		setIfUnset(properties, prefix + ACCESS_TOKEN_FORMAT, AccessTokenFormat.standard.toString());
		setIfUnset(properties, prefix + ICON_URL, "file:../common/img/external/linkedin-small.png");
		setDefaultProfileIfUnset(properties, prefix, "sys:linkedin");
		return properties;
	}
}
