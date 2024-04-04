/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;

import java.util.Properties;

/**
 * Preset configuration for Microsoft OAuth provider.
 * @author K. Benedyczak
 */
public class MicrosoftLiveProviderProperties extends CustomProviderProperties
{

	public MicrosoftLiveProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "Microsoft Live");
		setIfUnset(properties, prefix + PROVIDER_LOCATION, "https://login.live.com/oauth20_authorize.srf");
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE, ClientAuthnMode.secretPost.toString());
		setIfUnset(properties, prefix + ACCESS_TOKEN_ENDPOINT, "https://login.live.com/oauth20_token.srf");
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://apis.live.net/v5.0/me");
		setIfUnset(properties, prefix + SCOPES, "wl.basic wl.birthday wl.emails wl.phone_numbers wl.postal_addresses wl.work_profile");
		setIfUnset(properties, prefix + ICON_URL, "assets/img/external/ms-small.png");
		setDefaultProfileIfUnset(properties, prefix, "sys:microsoftLive");
		return properties;
	}

}
