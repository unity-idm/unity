/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.client.console;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.SigningAlgorithms;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.oauth.client.config.RequestACRsMode;

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
	private String federationTrustAnchorJwks;
	private int federationMetadataValidity;
	private String federationTruststore;
	private String federationHostnameCheckingMode;
	private SigningAlgorithms federationJwtSigningAlgorithm;
	private TranslationProfile federationProviderTranslationProfile;
	private String federationProviderRegistrationForm;
	private RequestACRsMode federationProviderRequestACRsMode;
	private List<String> federationProviderRequestedACRs;
	private boolean federationProviderRequestedACRsAreEssential;

	public OAuthConfiguration()
	{
		providers = new ArrayList<>();
		defAccountAssociation = true;
		federationMetadataValidity = OAuthClientProperties.DEFAULT_FEDERATION_METADATA_VALIDITY;
		federationHostnameCheckingMode = ServerHostnameCheckingMode.FAIL.name();
		federationProviderTranslationProfile = TranslationProfileGenerator
				.generateIncludeInputProfile(OAuthClientProperties.DEFAULT_TRANSLATION_PROFILE_FOR_FEDERATION_CLIENT);
		federationProviderRequestACRsMode = RequestACRsMode.NONE;
		federationProviderRequestedACRs = new ArrayList<>();
		federationProviderRequestedACRsAreEssential = false;

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
		federationTrustAnchorJwks = oauthProp.getValue(OAuthClientProperties.FEDERATION_TRUST_ANCHOR_JWKS);
		String federationJwtSigningAlgStr = oauthProp.getValue(OAuthClientProperties.FEDERATION_JWT_SIGNING_ALG);
		federationJwtSigningAlgorithm = federationJwtSigningAlgStr != null && !federationJwtSigningAlgStr.isEmpty()
				? SigningAlgorithms.valueOf(federationJwtSigningAlgStr) : null;
		federationMetadataValidity = oauthProp.getIntValue(OAuthClientProperties.FEDERATION_METADATA_VALIDITY);
		federationTruststore = oauthProp.getValue(OAuthClientProperties.FEDERATION_TRUSTSTORE);
		ServerHostnameCheckingMode checkingMode = oauthProp.getEnumValue(
				OAuthClientProperties.FEDERATION_HOSTNAME_CHECKING, ServerHostnameCheckingMode.class);
		federationHostnameCheckingMode = checkingMode != null ? checkingMode.name()
				: ServerHostnameCheckingMode.FAIL.name();
		if (oauthProp.isSet(OAuthClientProperties.FEDERATION_EMBEDDED_TRANSLATION_PROFILE))
			federationProviderTranslationProfile = TranslationProfileGenerator.getProfileFromString(
					oauthProp.getValue(OAuthClientProperties.FEDERATION_EMBEDDED_TRANSLATION_PROFILE));
		else if (oauthProp.isSet(OAuthClientProperties.FEDERATION_TRANSLATION_PROFILE))
			federationProviderTranslationProfile = TranslationProfileGenerator.generateIncludeInputProfile(
					oauthProp.getValue(OAuthClientProperties.FEDERATION_TRANSLATION_PROFILE));
		federationProviderRegistrationForm = oauthProp.getValue(OAuthClientProperties.FEDERATION_REGISTRATION_FORM);
		federationProviderRequestACRsMode = oauthProp.getEnumValue(
				OAuthClientProperties.FEDERATION_REQUEST_ACRS_MODE, RequestACRsMode.class);
		federationProviderRequestedACRs = oauthProp.getListOfValues(OAuthClientProperties.FEDERATION_REQUESTED_ACRS);
		federationProviderRequestedACRsAreEssential = oauthProp.getBooleanValue(
				OAuthClientProperties.FEDERATION_REQUESTED_ACRS_ARE_ESSENTIAL);

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

		
		
		raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_MEMBERSHIP_ENABLED,
				String.valueOf(federationMembershipEnabled));
		
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
			if (federationTrustAnchorJwks != null && !federationTrustAnchorJwks.isEmpty())
			{
				raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_TRUST_ANCHOR_JWKS, federationTrustAnchorJwks);
			}
			if (federationJwtSigningAlgorithm != null)
			{
				raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_JWT_SIGNING_ALG,
						federationJwtSigningAlgorithm.name());
			}
			raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_METADATA_VALIDITY,
					String.valueOf(federationMetadataValidity));
			if (federationTruststore != null && !federationTruststore.isEmpty())
				raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_TRUSTSTORE,
						federationTruststore);
			if (federationHostnameCheckingMode != null && !federationHostnameCheckingMode.isEmpty())
				raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_HOSTNAME_CHECKING,
						federationHostnameCheckingMode);
			if (federationProviderTranslationProfile != null)
			{
				try
				{
					raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_EMBEDDED_TRANSLATION_PROFILE,
							new ObjectMapper().writeValueAsString(federationProviderTranslationProfile.toJsonObject()));
				} catch (Exception e)
				{
					throw new InternalException("Can't serialize federation translation profile to JSON", e);
				}
			}
			if (federationProviderRegistrationForm != null && !federationProviderRegistrationForm.isEmpty())
				raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_REGISTRATION_FORM,
						federationProviderRegistrationForm);
			if (federationProviderRequestACRsMode != null)
				raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_REQUEST_ACRS_MODE,
						federationProviderRequestACRsMode.name());
			if (federationProviderRequestedACRs != null)
				for (int i = 0; i < federationProviderRequestedACRs.size(); i++)
					raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_REQUESTED_ACRS + i,
							federationProviderRequestedACRs.get(i));
			raw.put(OAuthClientProperties.P + OAuthClientProperties.FEDERATION_REQUESTED_ACRS_ARE_ESSENTIAL,
					String.valueOf(federationProviderRequestedACRsAreEssential));
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

	public String getFederationTrustAnchorJwks()
	{
		return federationTrustAnchorJwks;
	}

	public void setFederationTrustAnchorJwks(String federationTrustAnchorJwks)
	{
		this.federationTrustAnchorJwks = federationTrustAnchorJwks;
	}

	public int getFederationMetadataValidity()
	{
		return federationMetadataValidity;
	}

	public void setFederationMetadataValidity(int federationMetadataValidity)
	{
		this.federationMetadataValidity = federationMetadataValidity;
	}

	public String getFederationTruststore()
	{
		return federationTruststore;
	}

	public void setFederationTruststore(String federationTruststore)
	{
		this.federationTruststore = federationTruststore;
	}

	public String getFederationHostnameCheckingMode()
	{
		return federationHostnameCheckingMode;
	}

	public void setFederationHostnameCheckingMode(String federationHostnameCheckingMode)
	{
		this.federationHostnameCheckingMode = federationHostnameCheckingMode;
	}

	public SigningAlgorithms getFederationJwtSigningAlgorithm()
	{
		return federationJwtSigningAlgorithm;
	}

	public void setFederationJwtSigningAlgorithm(SigningAlgorithms federationJwtSigningAlgorithm)
	{
		this.federationJwtSigningAlgorithm = federationJwtSigningAlgorithm;
	}

	public TranslationProfile getFederationProviderTranslationProfile()
	{
		return federationProviderTranslationProfile;
	}

	public void setFederationProviderTranslationProfile(TranslationProfile federationProviderTranslationProfile)
	{
		this.federationProviderTranslationProfile = federationProviderTranslationProfile;
	}

	public String getFederationProviderRegistrationForm()
	{
		return federationProviderRegistrationForm;
	}

	public void setFederationProviderRegistrationForm(String federationProviderRegistrationForm)
	{
		this.federationProviderRegistrationForm = federationProviderRegistrationForm;
	}

	public RequestACRsMode getFederationProviderRequestACRsMode()
	{
		return federationProviderRequestACRsMode;
	}

	public void setFederationProviderRequestACRsMode(RequestACRsMode federationProviderRequestACRsMode)
	{
		this.federationProviderRequestACRsMode = federationProviderRequestACRsMode;
	}

	public List<String> getFederationProviderRequestedACRs()
	{
		return federationProviderRequestedACRs;
	}

	public void setFederationProviderRequestedACRs(List<String> federationProviderRequestedACRs)
	{
		this.federationProviderRequestedACRs = federationProviderRequestedACRs;
	}

	public boolean isFederationProviderRequestedACRsAreEssential()
	{
		return federationProviderRequestedACRsAreEssential;
	}

	public void setFederationProviderRequestedACRsAreEssential(boolean federationProviderRequestedACRsAreEssential)
	{
		this.federationProviderRequestedACRsAreEssential = federationProviderRequestedACRsAreEssential;
	}
}