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
public class DropboxProviderProperties extends CustomProviderProperties
{

	public DropboxProviderProperties(Properties properties, String prefix, PKIManagement pkiManagement) 
			throws ConfigurationException
	{
		super(addDefaults(properties, prefix), prefix, pkiManagement);
	}
	
	private static Properties addDefaults(Properties properties, String prefix)
	{
		setIfUnset(properties, prefix + PROVIDER_NAME, "Dropbox");
		setIfUnset(properties, prefix + PROVIDER_LOCATION, "https://www.dropbox.com/oauth2/authorize");
		setIfUnset(properties, prefix + CLIENT_AUTHN_MODE, ClientAuthnMode.secretBasic.toString());
		setIfUnset(properties, prefix + ACCESS_TOKEN_ENDPOINT, "https://api.dropboxapi.com/oauth2/token");
		setIfUnset(properties, prefix + CLIENT_HTTP_METHOD_FOR_PROFILE_ACCESS, ClientHttpMethod.post.toString());
		setIfUnset(properties, prefix + PROFILE_ENDPOINT, "https://api.dropboxapi.com/2/users/get_current_account");
		setIfUnset(properties, prefix + ACCESS_TOKEN_FORMAT, AccessTokenFormat.standard.toString());
		setIfUnset(properties, prefix + ICON_URL, "file:../common/img/external/dropbox-small.png");
		setIfUnset(properties, prefix + CommonWebAuthnProperties.TRANSLATION_PROFILE, "sys:dropbox");
		return properties;
	}

}
