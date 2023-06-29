/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class OAuthConfiguration
{
	private boolean defAccountAssociation;
	private List<OAuthProviderConfiguration> providers;

	public OAuthConfiguration()
	{
		providers = new ArrayList<>();
		defAccountAssociation = true;
	}

	public void fromProperties(String properties, MessageSource msg, PKIManagement pkiMan, 
			ImageAccessService imageAccessService)
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
		defAccountAssociation = oauthProp.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION);

		providers.clear();
		Set<String> keys = oauthProp.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
		for (String key : keys)
		{
			String idpKey = key.substring(OAuthClientProperties.PROVIDERS.length(), key.length() - 1);

			OAuthProviderConfiguration provider = new OAuthProviderConfiguration();
			CustomProviderProperties providerProps = oauthProp.getProvider(key);
			provider.fromProperties(msg, imageAccessService,  providerProps, idpKey);
			providers.add(provider);
		}
	}

	public String toProperties(MessageSource msg, PKIManagement pkiMan, FileStorageService fileStorageService, 
			String authName) throws ConfigurationException
	{
		Properties raw = new Properties();

		raw.put(OAuthClientProperties.P + CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION,
				String.valueOf(defAccountAssociation));

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

	public boolean isDefAccountAssociation()
	{
		return defAccountAssociation;
	}

	public void setDefAccountAssociation(boolean accountAssociation)
	{
		this.defAccountAssociation = accountAssociation;
	}
}