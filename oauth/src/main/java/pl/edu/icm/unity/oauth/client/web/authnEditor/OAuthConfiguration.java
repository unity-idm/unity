/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.web.authnEditor;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;

/**
 * 
 * @author P.Piernik
 *
 */
public class OAuthConfiguration
{
	private boolean accountAssociation;
	private List<OAuthProviderConfiguration> providers;

	public OAuthConfiguration()
	{
		providers = new ArrayList<>();
	}

	public void fromProperties(String properties, UnityMessageSource msg, PKIManagement pkiMan, URIAccessService uriAccessService)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the oauth2 verificator", e);
		}

		OAuthClientProperties oauthProp = new OAuthClientProperties(raw, pkiMan);
		accountAssociation = oauthProp.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION);

		providers.clear();
		Set<String> keys = oauthProp.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
		for (String key : keys)
		{
			String idpKey = key.substring(OAuthClientProperties.PROVIDERS.length(), key.length() - 1);

			OAuthProviderConfiguration provider = new OAuthProviderConfiguration();
			CustomProviderProperties providerProps = oauthProp.getProvider(key);
			provider.fromProperties(msg, uriAccessService,  providerProps, idpKey);
			providers.add(provider);
		}
	}

	public String toProperties(UnityMessageSource msg, PKIManagement pkiMan, FileStorageService fileStorageService, String authName) throws ConfigurationException
	{
		Properties raw = new Properties();

		raw.put(OAuthClientProperties.P + CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION,
				String.valueOf(accountAssociation));

		for (OAuthProviderConfiguration provider : providers)
		{
			provider.toProperties(raw, msg, fileStorageService, authName);
		}

		OAuthClientProperties prop = new OAuthClientProperties(raw, pkiMan);
		return prop.getAsString();
	}

	public void setProviders(List<OAuthProviderConfiguration> configurations)
	{
		providers.clear();
		providers.addAll(configurations);
	}

	public List<OAuthProviderConfiguration> getProviders()
	{
		return providers;
	}

	public boolean isAccountAssociation()
	{
		return accountAssociation;
	}

	public void setAccountAssociation(boolean accountAssociation)
	{
		this.accountAssociation = accountAssociation;
	}
}