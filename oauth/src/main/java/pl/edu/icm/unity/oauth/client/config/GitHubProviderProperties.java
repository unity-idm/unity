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
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE, ClientAuthnMode.secretPost.toString());
		setIfUnset(properties, prefix + ACCESS_TOKEN_ENDPOINT, "https://github.com/login/oauth/access_token");
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://api.github.com/user");
		setIfUnset(properties, prefix + SCOPES, "read:user user:email");
		setIfUnset(properties, prefix + ACCESS_TOKEN_FORMAT, AccessTokenFormat.httpParams.toString());
		setIfUnset(properties, prefix + ICON_URL, "file:../common/img/external/github-small.png");
		setIfUnset(properties, prefix + CommonWebAuthnProperties.TRANSLATION_PROFILE, "sys:github");
		return properties;
	}

}
