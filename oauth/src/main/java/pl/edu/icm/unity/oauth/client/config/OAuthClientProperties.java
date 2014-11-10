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

import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.webui.common.idpselector.IdpSelectorComponent.ScaleMode;
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
	
	public enum Providers {custom, google, facebook, dropbox, github, microsoft};
	
	@DocumentationReferencePrefix
	public static final String P = "unity.oauth2.client.";

	public static final String DISPLAY_NAME = "displayName";
	public static final String PROVIDERS_IN_ROW = "providersInRow";
	public static final String ICON_SCALE = "iconScale";
	
	public static final String PROVIDERS = "providers.";

	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static 
	{
		META.put(DISPLAY_NAME, new PropertyMD("OAuth2 authentication").setDescription("Name of this authentication "
				+ "option to be displayed in the web interface"));
		META.put(PROVIDERS_IN_ROW, new PropertyMD("3").setPositive().setDescription("How many providers should be displayed "
				+ "in a single row on the provider selection screen. Relevant only if you define multiple providers."));
		META.put(ICON_SCALE, new PropertyMD(ScaleMode.none).setDescription("Controls whether and how "
				+ "the icons of providers should be scalled."));
		
		META.put(PROVIDERS, new PropertyMD().setStructuredList(false).setCanHaveSubkeys().setMandatory().
				setDescription("Prefix, under which the available oauth providers are defined."));

		META.put(CustomProviderProperties.PROVIDER_TYPE, new PropertyMD(Providers.custom).setHidden().
				setStructuredListEntry(PROVIDERS));
	}
	
	private Map<String, CustomProviderProperties> providers = new HashMap<String, CustomProviderProperties>();
	
	
	public OAuthClientProperties(Properties properties, PKIManagement pkiManagement) throws ConfigurationException
	{
		super(P, properties, META, log);
		Set<String> keys = getStructuredListKeys(PROVIDERS);
		for (String key: keys)
			setupProvider(key, pkiManagement);
	}

	private void setupProvider(String key, PKIManagement pkiManagement)
	{
		Providers providerType = getEnumValue(key+CustomProviderProperties.PROVIDER_TYPE, Providers.class);
		switch (providerType)
		{
		case google:
			providers.put(key, new GoogleProviderProperties(properties, P+key, pkiManagement));
			break;
		case facebook:
			providers.put(key, new FacebookProviderProperties(properties, P+key, pkiManagement));
			break;
		case dropbox:
			providers.put(key, new DropboxProviderProperties(properties, P+key, pkiManagement));
			break;
		case github:
			providers.put(key, new GitHubProviderProperties(properties, P+key, pkiManagement));
			break;
		case microsoft:
			providers.put(key, new MicrosoftProviderProperties(properties, P+key, pkiManagement));
			break;
		case custom:
			providers.put(key, new CustomProviderProperties(properties, P+key, pkiManagement));
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
