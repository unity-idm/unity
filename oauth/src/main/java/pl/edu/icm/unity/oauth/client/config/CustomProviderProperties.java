/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of OAuth client for custom provider.
 * @author K. Benedyczak
 */
public class CustomProviderProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, CustomProviderProperties.class);
	
	public enum ClientAuthnMode {secretPost, secretBasic};
	
	@DocumentationReferencePrefix
	public static final String P = "unity.oauth2.client.CLIENT_ID.";

	public static final String PROVIDER_LOCATION = "authEndpoint";
	public static final String ACCESS_TOKEN_ENDPOINT = "accessTokenEndpoint";
	public static final String PROFILE_ENDPOINT = "profileEndpoint";
	public static final String PROVIDER_NAME = "name";
	public static final String CLIENT_ID = "clientId";
	public static final String CLIENT_SECRET = "clientSecret";
	public static final String CLIENT_AUTHN_MODE = "clientAuthenticationMode";
	public static final String SCOPES = "scopes";
	public static final String OPENID_CONNECT = "openIdConnect";
	public static final String OPENID_DISCOVERY = "openIdConnectDiscoveryEndpoint";
	public static final String REGISTRATION_FORM = "registrationFormForUnknown";
	public static final String TRANSLATION_PROFILE = "translationProfile";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static 
	{
		META.put(PROVIDER_LOCATION, new PropertyMD().setMandatory().
				setDescription("Location (URL) of OAuth2 provider's authorization endpoint."));
		META.put(ACCESS_TOKEN_ENDPOINT, new PropertyMD().
				setDescription("Location (URL) of OAuth2 provider's access token endpoint. "
						+ "In case of OpenID Connect mode can be discovered, otherwise mandatory."));
		META.put(PROFILE_ENDPOINT, new PropertyMD().
				setDescription("Location (URL) of OAuth2 provider's user's profile endpoint. "
						+ "It is used to obtain additional user's attributes. "
						+ "It can be autodiscovered for OpenID Connect mode. Otherwise it must be"
						+ " set as otherwise there is no information about the user identity."));
		META.put(PROVIDER_NAME, new PropertyMD().setMandatory().
				setDescription("Name of the OAuth provider to be displayed"));
		META.put(CLIENT_ID, new PropertyMD().setMandatory().
				setDescription("Client identifier, obtained during Unity's "
				+ "registration at the provider"));
		META.put(CLIENT_SECRET, new PropertyMD().setSecret().setMandatory().
				setDescription("Client secret, obtained during Unity's "
				+ "registration at the provider"));
		META.put(CLIENT_AUTHN_MODE, new PropertyMD(ClientAuthnMode.secretBasic).
				setDescription("Defines how the client secret and id should be passed to the provider."));
		META.put(SCOPES, new PropertyMD().
				setDescription("Space separated list of authorization scopes to be requested. "
						+ "Most often required if in non OpenID Connect mode, otherwise has a default "
						+ "value of 'openid email'"));
		META.put(OPENID_CONNECT, new PropertyMD("false").
				setDescription("If set to true, then the provider is treated as OpenID "
						+ "Connect 1.0 provider. For such providers specifying " + 
						PROFILE_ENDPOINT + " is not mandatory as the basic user information "
						+ "is retrieved together with access token. However the " 
						+ "discovery endpoint must be set."));
		META.put(OPENID_DISCOVERY, new PropertyMD().
				setDescription("OpenID Connect Discovery endpoint address, relevant (and required) "
						+ "only when OpenID Connect mode is turned on."));
		META.put(REGISTRATION_FORM, new PropertyMD().
				setDescription("Registration form to be shown for the locally unknown users which "
						+ "were successfuly authenticated remotely."));
		META.put(TRANSLATION_PROFILE, new PropertyMD().setMandatory().
				setDescription("Translation profile which will be used to map received user "
						+ "information to a local representation."));
	}
	
	public CustomProviderProperties(Properties properties, String prefix) throws ConfigurationException
	{
		super(prefix, properties, META, log);
		boolean openIdConnect = getBooleanValue(OPENID_CONNECT);
		if (openIdConnect)
		{
			if (!isSet(SCOPES))
				setProperty(SCOPES, "openid email");
			if (!isSet(OPENID_DISCOVERY))
				throw new ConfigurationException(getKeyDescription(OPENID_DISCOVERY) + 
						" is mandatory in OpenID Connect mode");
			
		} else
		{
			if (!isSet(ACCESS_TOKEN_ENDPOINT))
				throw new ConfigurationException(getKeyDescription(ACCESS_TOKEN_ENDPOINT) + 
						" is mandatory in non OpenID Connect mode");
			if (!isSet(PROFILE_ENDPOINT))
				throw new ConfigurationException(getKeyDescription(PROFILE_ENDPOINT) + 
						" is mandatory in non OpenID Connect mode");
		}
	}

	public Properties getProperties()
	{
		return properties;
	}
}
