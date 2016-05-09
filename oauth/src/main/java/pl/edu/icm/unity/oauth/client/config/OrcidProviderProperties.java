/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.Properties;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.server.api.PKIManagement;

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
		properties.setProperty(prefix + PROVIDER_NAME, "ORCID");
		properties.setProperty(prefix + PROVIDER_LOCATION, "https://orcid.org/oauth/authorize");
		properties.setProperty(prefix + ACCESS_TOKEN_ENDPOINT, "https://pub.orcid.org/oauth/token");
		properties.setProperty(prefix + ICON_URL, "file:../common/img/external/orcid-small.png");
		properties.setProperty(prefix + SCOPES, "/authenticate");
		return properties;
	}

}
