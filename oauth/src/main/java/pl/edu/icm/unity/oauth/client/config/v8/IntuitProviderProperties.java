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
		setIfUnset(properties, prefix + ICON_URL, "file:../common/img/external/intuit-small.png");
		setDefaultProfileIfUnset(properties, prefix, "sys:intuit");
		return properties;
	}

}
