/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;

import java.util.Properties;

/**
 * Preset configuration for Intuit (company behind Quick books) OIDC provider.
 * 
 * @author K. Benedyczak
 */
public class IntuitProviderProperties extends CustomProviderProperties
{

	public IntuitProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "Intuit");
		setIfUnset(properties, prefix + OPENID_CONNECT, "true");
		setIfUnset(properties, prefix + OPENID_DISCOVERY,
				"https://developer.api.intuit.com/.well-known/openid_configuration");
		setIfUnset(properties, prefix + SCOPES, "openid email profile");
		setIfUnset(properties, prefix + ICON_URL, "assets/img/external/intuit-small.png");
		setDefaultProfileIfUnset(properties, prefix, "sys:intuit");
		return properties;
	}

}
