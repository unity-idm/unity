/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;

import java.util.Properties;

/**
 * Preset configuration for Unity provider.
 * @author K. Benedyczak
 */
public class UnityProviderProperties extends CustomProviderProperties
{

	public UnityProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "UnityIdM");
		setIfUnset(properties, prefix + ICON_URL, "assets/img/other/logo-square.png");
		setDefaultProfileIfUnset(properties, prefix, "sys:oidc");
		setIfUnset(properties, prefix + OPENID_CONNECT, "true");
		return properties;
	}

}
