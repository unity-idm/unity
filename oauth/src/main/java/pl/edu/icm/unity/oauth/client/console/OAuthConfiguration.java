/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;

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
	private boolean federationMembershipEnabled;
	private String federationCredential;
	private String federationSuperiorEntityId;
	private String authenticationCredential;
	private String federationTrustAnchorId;
	private String federationJwks;
	private int federationMetadataValidity;
	
	public OAuthConfiguration()
	{
		providers = new ArrayList<>();
		defAccountAssociation = true;
		federationMetadataValidity = OAuthClientProperties.DEFAULT_FEDERATION_METADATA_VALIDITY;
	}

	public void fromProperties(String properties, MessageSource msg, PKIManagement pkiMan,
			VaadinLogoImageLoader imageAccessService)
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
		
		federationMembershipEnabled = oauthProp.getBooleanValue(OAuthClientProperties.FEDERATION_MEMBERSHIP_ENABLED);
		federationCredential = oauthProp.getValue(OAuthClientProperties.FEDERATION_CREDENTIAL);
		federationSuperiorEntityId = oauthProp.getValue(OAuthClientProperties.FEDERATION_SUPERIOR_ENTITY_ID);
		authenticationCredential = oauthProp.getValue(OAuthClientProperties.AUTHENTICATION_CREDENTIAL);
		federationTrustAnchorId = oauthProp.getValue(OAuthClientProperties.FEDERATION_TRUST_ANCHOR_ID);
		federationJwks = oauthProp.getValue(OAuthClientProperties.FEDERATION_JWKS);
		federationMetadataValidity = oauthProp.getIntValue(OAuthClientProperties.FEDERATION_METADATA_VALIDITY);
		
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

		
		
		raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_MEMBERSHIP_ENABLED, String.valueOf(federationMembershipEnabled));
		
		if(federationMembershipEnabled)
		{
			if (federationCredential != null && !federationCredential.isEmpty())
			{
				raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_CREDENTIAL, federationCredential);
			}

			if (federationSuperiorEntityId != null && !federationSuperiorEntityId.isEmpty())
			{
				raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_SUPERIOR_ENTITY_ID,
						federationSuperiorEntityId);
			}
			if (authenticationCredential != null && !authenticationCredential.isEmpty())
			{
				raw.put(OAuthClientProperties.P + OAuthClientProperties.AUTHENTICATION_CREDENTIAL, authenticationCredential);
			}
			if (federationTrustAnchorId != null && !federationTrustAnchorId.isEmpty())
			{
				raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_TRUST_ANCHOR_ID, federationTrustAnchorId);
			}
			if (federationJwks != null && !federationJwks.isEmpty())
			{
				raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_JWKS, federationJwks);
			}
			raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_METADATA_VALIDITY,
					String.valueOf(federationMetadataValidity));
		}
		
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
	
	public boolean isFederationMembershipEnabled()
	{
		return federationMembershipEnabled;
	}

	public void setFederationMembershipEnabled(boolean federationMembershipEnabled)
	{
		this.federationMembershipEnabled = federationMembershipEnabled;
	}

	public String getFederationCredential()
	{
		return federationCredential;
	}

	public void setFederationCredential(String federationCredential)
	{
		this.federationCredential = federationCredential;
	}

	public String getFederationSuperiorEntityId()
	{
		return federationSuperiorEntityId;
	}

	public void setFederationSuperiorEntityId(String federationSuperiorEntityId)
	{
		this.federationSuperiorEntityId = federationSuperiorEntityId;
	}

	public String getAuthenticationCredential()
	{
		return authenticationCredential;
	}

	public void setAuthenticationCredential(String authenticationCredential)
	{
		this.authenticationCredential = authenticationCredential;
	}

	public String getFederationTrustAnchorId()
	{
		return federationTrustAnchorId;
	}

	public void setFederationTrustAnchorId(String federationTrustAnchorId)
	{
		this.federationTrustAnchorId = federationTrustAnchorId;
	}

	public String getFederationJwks()
	{
		return federationJwks;
	}

	public void setFederationJwks(String federationJwks)
	{
		this.federationJwks = federationJwks;
	}

	public int getFederationMetadataValidity()
	{
		return federationMetadataValidity;
	}

	public void setFederationMetadataValidity(int federationMetadataValidity)
	{
		this.federationMetadataValidity = federationMetadataValidity;
	}
}