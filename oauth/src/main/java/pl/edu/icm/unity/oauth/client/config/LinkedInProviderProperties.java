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
		setIfUnset(properties, prefix + PROVIDER_LOCATION, "https://www.linkedin.com/uas/oauth2/authorization");
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE, ClientAuthnMode.secretPost.toString());
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS, ClientAuthnMode.secretBasic.toString());
		setIfUnset(properties, prefix + ACCESS_TOKEN_ENDPOINT, "https://www.linkedin.com/uas/oauth2/accessToken");
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://api.linkedin.com/v1/people/~:"
				+ "(id,firstName,lastName,emailAddress,location,pictureUrl,publicProfileURL)?format=json");
		setIfUnset(properties, prefix + SCOPES, "r_basicprofile r_emailaddress");
		setIfUnset(properties, prefix + ACCESS_TOKEN_FORMAT, AccessTokenFormat.standard.toString());
		setIfUnset(properties, prefix + ICON_URL, "file:../common/img/external/linkedin-small.png");
		setIfUnset(properties, prefix + CommonWebAuthnProperties.TRANSLATION_PROFILE, "sys:linkedin");
		return properties;
	}
}
