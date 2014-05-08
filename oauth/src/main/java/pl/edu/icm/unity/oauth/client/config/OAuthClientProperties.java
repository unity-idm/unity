/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of OAuth client.
 * @author K. Benedyczak
 */
public class OAuthClientProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, OAuthClientProperties.class);
	
	public enum Providers {custom, google, facebook, dropbox, github};
	
	@DocumentationReferencePrefix
	public static final String P = "unity.oauth2.client.";

	public static final String DISPLAY_NAME = "displayName";
	
	public static final String PROVIDERS = "providers.";
	
	public static final String PROVIDER_TYPE = "type";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static 
	{
		META.put(DISPLAY_NAME, new PropertyMD("OAuth2 authentication").setDescription("Name of this authentication "
				+ "option to be displayed in the web interface"));
		
		META.put(PROVIDERS, new PropertyMD().setStructuredList(false).setMandatory().
				setDescription("Prefix, under which the available oauth providers are defined."));
		
		META.put(PROVIDER_TYPE, new PropertyMD(Providers.custom).setStructuredListEntry(PROVIDERS).
				setDescription("Type of provider. Either a well known provider type can be specified"
						+ " or 'custom'. In the first case only few additional settings are required: "
						+ "client id, secret and translation profile. Other settings as scope "
						+ "can be additionally set to fine tune the remote authentication. "
						+ "In the latter 'custom' case all mandatory options must be set."));
		
	}
	
	private Map<String, CustomProviderProperties> providers = new HashMap<String, CustomProviderProperties>();
	
	public OAuthClientProperties(Properties properties) throws ConfigurationException
	{
		super(P, properties, META, log);
		Set<String> keys = getStructuredListKeys(PROVIDERS);
		for (String key: keys)
			setupProvider(key);
	}

	private void setupProvider(String key)
	{
		Providers providerType = getEnumValue(key+PROVIDER_TYPE, Providers.class);
		switch (providerType)
		{
		case google:
			providers.put(key, new GoogleProviderProperties(properties, key));
			break;
		case facebook:
			providers.put(key, new FacebookProviderProperties(properties, key));
			break;
		case dropbox:
			providers.put(key, new DropboxProviderProperties(properties, key));
			break;
		case github:
			providers.put(key, new GitHubProviderProperties(properties, key));
			break;
		case custom:
			providers.put(key, new CustomProviderProperties(properties, key));
		}
	}
	
	public CustomProviderProperties getProvider(String key)
	{
		return providers.get(key);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
