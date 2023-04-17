/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Configuration of OAuth client.
 * @author K. Benedyczak
 */
public class OAuthClientProperties extends UnityPropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, OAuthClientProperties.class);

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

	private Map<String, CustomProviderProperties> v8Providers = new HashMap<>();
	private Map<String, CustomProviderProperties> providers = new HashMap<>();


	public OAuthClientProperties(Properties properties, PKIManagement pkiManagement) throws ConfigurationException
	{
		super(P, properties, META, log);
		Set<String> keys = getStructuredListKeys(PROVIDERS);
		Properties clone = (Properties)properties.clone();
		for (String key: keys)
		{
			setupV8Provider(key, pkiManagement);
			setupProvider(key, pkiManagement, clone);
		}
	}

	private void setupV8Provider(String key, PKIManagement pkiManagement)
	{
		Providers providerType = getEnumValue(key+CustomProviderProperties.PROVIDER_TYPE, Providers.class);
		switch (providerType)
		{
		case google:
			v8Providers.put(key, new pl.edu.icm.unity.oauth.client.config.v8.GoogleProviderProperties(properties, P+key, pkiManagement));
			break;
		case facebook:
			v8Providers.put(key, new pl.edu.icm.unity.oauth.client.config.v8.FacebookProviderProperties(properties, P+key, pkiManagement));
			break;
		case dropbox:
			v8Providers.put(key, new pl.edu.icm.unity.oauth.client.config.v8.DropboxProviderProperties(properties, P+key, pkiManagement));
			break;
		case github:
			v8Providers.put(key, new pl.edu.icm.unity.oauth.client.config.v8.GitHubProviderProperties(properties, P+key, pkiManagement));
			break;
		case microsoft:
			v8Providers.put(key, new pl.edu.icm.unity.oauth.client.config.v8.MicrosoftLiveProviderProperties(properties, P+key, pkiManagement));
			break;
		case microsoftAzureV2:
			v8Providers.put(key, new pl.edu.icm.unity.oauth.client.config.v8.MicrosoftAzureV2ProviderProperties(properties, P+key, pkiManagement));
			break;
		case orcid:
			v8Providers.put(key, new pl.edu.icm.unity.oauth.client.config.v8.OrcidProviderProperties(properties, P+key, pkiManagement));
			break;
		case linkedin:
			v8Providers.put(key, new pl.edu.icm.unity.oauth.client.config.v8.LinkedInProviderProperties(properties, P+key, pkiManagement));
			break;
		case unity:
			v8Providers.put(key, new pl.edu.icm.unity.oauth.client.config.v8.UnityProviderProperties(properties, P+key, pkiManagement));
			break;
		case intuit:
			v8Providers.put(key, new pl.edu.icm.unity.oauth.client.config.v8.IntuitProviderProperties(properties, P+key, pkiManagement));
			break;
		case custom:
			v8Providers.put(key, new CustomProviderProperties(properties, P+key, pkiManagement));
		}
	}

	private void setupProvider(String key, PKIManagement pkiManagement, Properties properties)
	{
		Providers providerType = getEnumValue(key+CustomProviderProperties.PROVIDER_TYPE, Providers.class);
		switch (providerType)
		{
			case google -> providers.put(key, new GoogleProviderProperties(properties, P + key, pkiManagement));
			case facebook -> providers.put(key, new FacebookProviderProperties(properties, P + key, pkiManagement));
			case dropbox -> providers.put(key, new DropboxProviderProperties(properties, P + key, pkiManagement));
			case github -> providers.put(key, new GitHubProviderProperties(properties, P + key, pkiManagement));
			case microsoft ->
					providers.put(key, new MicrosoftLiveProviderProperties(properties, P + key, pkiManagement));
			case microsoftAzureV2 ->
					providers.put(key, new MicrosoftAzureV2ProviderProperties(properties, P + key, pkiManagement));
			case orcid -> providers.put(key, new OrcidProviderProperties(properties, P + key, pkiManagement));
			case linkedin -> providers.put(key, new LinkedInProviderProperties(properties, P + key, pkiManagement));
			case unity -> providers.put(key, new UnityProviderProperties(properties, P + key, pkiManagement));
			case intuit -> providers.put(key, new IntuitProviderProperties(properties, P + key, pkiManagement));
			case custom -> providers.put(key, new CustomProviderProperties(properties, P + key, pkiManagement));
		}
	}

	public CustomProviderProperties getProvider(String key)
	{
		return providers.get(key);
	}

	public CustomProviderProperties getV8Provider(String key)
	{
		return v8Providers.get(key);
	}

	public Properties getProperties()
	{
		return properties;
	}
}
