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

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;

/**
 * Configuration of OAuth client.
 * @author K. Benedyczak
 */
public class OAuthClientProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, OAuthClientProperties.class);
	
	public enum Providers {custom, google, facebook, dropbox, github, microsoft, microsoftAzureV2, orcid, linkedin, unity, intuit};
	
	@DocumentationReferencePrefix
	public static final String P = "unity.oauth2.client.";

	public static final String PROVIDERS = "providers.";

	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static 
	{
		META.put(PROVIDERS, new PropertyMD().setStructuredList(false).setCanHaveSubkeys().setMandatory().
				setDescription("Prefix, under which the available oauth providers are defined."));
		META.put(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION, new PropertyMD("true").
				setDescription("Default setting allowing to globally control whether "
				+ "account association feature is enabled. "
				+ "Used for those providers, for which the setting is not set explicitly."));
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
			providers.put(key, new MicrosoftLiveProviderProperties(properties, P+key, pkiManagement));
			break;
		case microsoftAzureV2:
			providers.put(key, new MicrosoftAzureV2ProviderProperties(properties, P+key, pkiManagement));
			break;
		case orcid:
			providers.put(key, new OrcidProviderProperties(properties, P+key, pkiManagement));
			break;
		case linkedin:
			providers.put(key, new LinkedInProviderProperties(properties, P+key, pkiManagement));
			break;
		case unity:
			providers.put(key, new UnityProviderProperties(properties, P+key, pkiManagement));
			break;
		case intuit:
			providers.put(key, new IntuitProviderProperties(properties, P+key, pkiManagement));
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
