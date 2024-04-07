/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;
import pl.edu.icm.unity.oauth.client.profile.PlainProfileFetcher;

import java.util.Properties;

/**
 * Preset configuration for Microsoft Azure AD V2 OAuth provider.
 * @author K. Benedyczak
 */
public class MicrosoftAzureV2ProviderProperties extends CustomProviderProperties
{

	public MicrosoftAzureV2ProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "Microsoft");
		setIfUnset(properties, prefix + OPENID_CONNECT, "true");
		setIfUnset(properties, prefix + OPENID_DISCOVERY, "https://login.microsoftonline.com/common/v2.0/.well-known/openid-configuration");
		setIfUnset(properties, prefix + SCOPES, "openid email profile https://graph.microsoft.com/user.read");
		setIfUnset(properties, prefix + ICON_URL, "assets/img/external/ms-small.png");
		setDefaultProfileIfUnset(properties, prefix, "sys:microsoftAzure-v2");
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://graph.microsoft.com/v1.0/me");
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE_FOR_PROFILE_ACCESS, "secretBasic");
		return properties;
	}

	@Override
	public UserProfileFetcher getUserAttributesResolver()
	{
		return new PlainProfileFetcher();
	}
}
