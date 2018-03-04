/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.client.UserProfileFetcher;
import pl.edu.icm.unity.oauth.client.profile.OrcidProfileFetcher;

/**
 * Preset configuration for Orcid OAuth provider.
 * This configuration is using a free API. For more complete authn, with user profile fetching 
 * an Orcid membership is required. This should work with customized configuration.
 * @author K. Benedyczak
 */
public class OrcidProviderProperties extends CustomProviderProperties
{

	public OrcidProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "ORCID");
		setIfUnset(properties, prefix + PROVIDER_LOCATION, "https://orcid.org/oauth/authorize");
		setIfUnset(properties, prefix + ACCESS_TOKEN_ENDPOINT, "https://pub.orcid.org/oauth/token");
		setIfUnset(properties, prefix + ICON_URL, "file:../common/img/external/orcid-small.png");
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://pub.orcid.org/v2.1/");
		setIfUnset(properties, prefix + SCOPES, "/authenticate");
		setIfUnset(properties, prefix + ADDITIONAL_AUTHZ_PARAMS + "1", "show_login=true");
		return properties;
	}

	@Override
	public UserProfileFetcher getUserAttributesResolver()
	{
		return new OrcidProfileFetcher();
	}
}
