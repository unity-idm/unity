/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;

import java.util.Properties;

/**
 * Preset configuration for Facebook OAuth provider.
 * @author K. Benedyczak
 */
public class GitHubProviderProperties extends CustomProviderProperties
{

	public GitHubProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement)
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "GitHub");
		setIfUnset(properties, prefix + PROVIDER_LOCATION, "https://github.com/login/oauth/authorize");
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE, ClientAuthnMode.secretBasic.toString());
		setIfUnset(properties, prefix + ACCESS_TOKEN_ENDPOINT, "https://github.com/login/oauth/access_token");
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://api.github.com/user");
		setIfUnset(properties, prefix + SCOPES, "read:user user:email");
		setIfUnset(properties, prefix + ACCESS_TOKEN_FORMAT, AccessTokenFormat.httpParams.toString());
		setIfUnset(properties, prefix + ICON_URL, "../unitygw/img/external/github-small.png");
		setDefaultProfileIfUnset(properties, prefix, "sys:github");
		return properties;
	}

}
