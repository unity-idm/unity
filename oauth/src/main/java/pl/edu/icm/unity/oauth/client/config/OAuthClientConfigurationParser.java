/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.config;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.nimbusds.jose.JWSAlgorithm;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;

@Component
public class OAuthClientConfigurationParser
{
	private final PKIManagement pkiManagement;
	private final MessageSource msg;

	public OAuthClientConfigurationParser(PKIManagement pkiManagement, MessageSource msg)
	{
		this.pkiManagement = pkiManagement;
		this.msg = msg;
	}

	public OAuthClientConfiguration parse(String source)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			return parse(properties);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the OAuth2 verificator(?)", e);
		}
	}

	public OAuthClientConfiguration parse(Properties source)
	{
		try
		{
			OAuthClientProperties props = new OAuthClientProperties(source, pkiManagement);
			return fromProperties(props, source);
		} catch (ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the OAuth2 verificator", e);
		}
	}

	private OAuthClientConfiguration fromProperties(OAuthClientProperties props, Properties raw)
	{
		boolean defaultEnableAssociation = props.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION);
		return OAuthClientConfiguration.builder()
				.withDefaultEnableAssociation(defaultEnableAssociation)
				.withAuthenticationCredential(props.getValue(OAuthClientProperties.AUTHENTICATION_CREDENTIAL))
				.withFederation(parseFederationConfig(props))
				.withFederationProviderDefaults(parseFederationProviderDefaults(props))
				.withProviders(parseProviders(props, defaultEnableAssociation))
				.withRawProperties(raw)
				.build();
	}

	private OAuthFederationConfig parseFederationConfig(OAuthClientProperties props)
	{
		String truststore = props.getValue(OAuthClientProperties.FEDERATION_TRUSTSTORE);
		ServerHostnameCheckingMode hostnameChecking = props.getEnumValue(
				OAuthClientProperties.FEDERATION_HOSTNAME_CHECKING, ServerHostnameCheckingMode.class);
		return OAuthFederationConfig.builder()
				.withEnabled(props.getBooleanValue(OAuthClientProperties.FEDERATION_MEMBERSHIP_ENABLED))
				.withCredential(props.getValue(OAuthClientProperties.FEDERATION_CREDENTIAL))
				.withSuperiorEntityId(props.getValue(OAuthClientProperties.FEDERATION_SUPERIOR_ENTITY_ID))
				.withTrustAnchorId(props.getValue(OAuthClientProperties.FEDERATION_TRUST_ANCHOR_ID))
				.withJwks(props.getValue(OAuthClientProperties.FEDERATION_TRUST_ANCHOR_JWKS))
				.withMetadataValidity(props.getIntValue(OAuthClientProperties.FEDERATION_METADATA_VALIDITY))
				.withTruststore(truststore)
				.withValidator(resolveFederationValidator(truststore))
				.withHostnameCheckingMode(hostnameChecking)
				.withJwtSigningAlgorithm(props.isSet(OAuthClientProperties.FEDERATION_JWT_SIGNING_ALG)
						? Optional.of(JWSAlgorithm.parse(props.getValue(OAuthClientProperties.FEDERATION_JWT_SIGNING_ALG)))
						: Optional.empty())
				.build();
	}

	private OAuthFederationProviderDefaults parseFederationProviderDefaults(OAuthClientProperties props)
	{
		return OAuthFederationProviderDefaults.builder()
				.withTranslationProfile(parseFederationTranslationProfile(props))
				.withRegistrationForm(props.getValue(OAuthClientProperties.FEDERATION_REGISTRATION_FORM))
				.build();
	}

	private OAuthProviders parseProviders(OAuthClientProperties props, boolean defaultEnableAssociation)
	{
		Set<String> keys = props.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
		List<OAuthProviderConfiguration> providerConfigs = new ArrayList<>();
		for (String rawKey : keys)
		{
			OAuthProviderKey providerKey = OAuthProviderKey.fromConfig(rawKey);
			CustomProviderProperties providerProps = props.getProvider(rawKey);
			providerConfigs.add(parseSingleProvider(providerKey, providerProps, defaultEnableAssociation));
		}
		return new OAuthProviders(providerConfigs);
	}

	public OAuthProviderConfiguration parseSingleProvider(OAuthProviderKey key,
			CustomProviderProperties p, boolean defaultEnableAssociation)
	{
		boolean enableAssociation = p.isSet(CommonWebAuthnProperties.ENABLE_ASSOCIATION)
				? p.getBooleanValue(CommonWebAuthnProperties.ENABLE_ASSOCIATION)
				: defaultEnableAssociation;

		Optional<ClientAuthnMode> clientAuthnMode = p.isSet(CustomProviderProperties.CLIENT_AUTHN_MODE)
				? Optional.of(p.getEnumValue(CustomProviderProperties.CLIENT_AUTHN_MODE, ClientAuthnMode.class))
				: Optional.empty();

		return OAuthProviderConfiguration.builder()
				.withKey(key)
				.withProviderType(p.getEnumValue(CustomProviderProperties.PROVIDER_TYPE,
						OAuthClientProperties.Providers.class))
				.withUserAttributesResolver(p.getUserAttributesResolver())
				.withName(p.getLocalizedString(msg, CustomProviderProperties.PROVIDER_NAME))
				.withIconUrl(p.isSet(CustomProviderProperties.ICON_URL)
						? p.getLocalizedString(msg, CustomProviderProperties.ICON_URL) : null)
				.withOpenIdConnect(p.getBooleanValue(CustomProviderProperties.OPENID_CONNECT))
				.withAuthorizationEndpoint(p.getValue(CustomProviderProperties.PROVIDER_LOCATION))
				.withAccessTokenEndpoint(p.getValue(CustomProviderProperties.ACCESS_TOKEN_ENDPOINT))
				.withUserInfoEndpoints(p.getUserInfoEndpoints())
				.withOpenIdDiscoveryEndpoint(p.getValue(CustomProviderProperties.OPENID_DISCOVERY))
				.withClientId(p.getValue(CustomProviderProperties.CLIENT_ID))
				.withClientSecret(p.getValue(CustomProviderProperties.CLIENT_SECRET))
				.withClientAuthnMethod(p.getEnumValue(CustomProviderProperties.CLIENT_AUTHN_METHOD,
						CustomProviderProperties.ClientAuthnMethod.class))
				.withClientCredential(p.getValue(CustomProviderProperties.CLIENT_CREDENTIAL))
				.withJwtSigningAlgorithm(p.isSet(CustomProviderProperties.CLIENT_JWT_SIGNING_ALG)
						? Optional.of(JWSAlgorithm.parse(p.getValue(CustomProviderProperties.CLIENT_JWT_SIGNING_ALG)))
						: Optional.empty())
				.withClientAuthnMode(clientAuthnMode)
				.withAccessTokenFormat(p.getEnumValue(CustomProviderProperties.ACCESS_TOKEN_FORMAT,
						CustomProviderProperties.AccessTokenFormat.class))
				.withTruststoreName(p.getValue(CustomProviderProperties.CLIENT_TRUSTSTORE))
				.withValidator(p.getValidator())
				.withHostNameCheckingMode(p.getHostNameCheckingMode())
				.withClientAuthnModeForProfileAccess(p.getClientAuthModeForProfileAccess())
				.withClientHttpMethodForProfileAccess(p.getClientHttpMethodForProfileAccess())
				.withScopes(p.getValue(CustomProviderProperties.SCOPES))
				.withAdditionalAuthzParams(p.getAdditionalAuthzParams())
				.withRequestACRsMode(p.getRequestACRMode())
				.withRequestedACRs(p.getListOfValues(CustomProviderProperties.REQUESTED_ACRS))
				.withRequestedACRsAreEssential(
						p.getBooleanValue(CustomProviderProperties.REQUESTED_ACRS_ARE_ESSENTIAL))
				.withTranslationProfile(parseTranslationProfile(p))
				.withRegistrationForm(p.getValue(CommonWebAuthnProperties.REGISTRATION_FORM))
				.withEnableAssociation(enableAssociation)
				.build();
	}

	private X509CertChainValidator resolveFederationValidator(String truststoreName)
	{
		if (truststoreName == null)
			return null;
		try
		{
			if (!pkiManagement.getValidatorNames().contains(truststoreName))
				throw new InternalException("Federation truststore " + truststoreName + " does not exist");
			return pkiManagement.getValidator(truststoreName);
		} catch (EngineException e)
		{
			throw new InternalException("Cannot resolve federation truststore " + truststoreName, e);
		}
	}

	private TranslationProfile parseTranslationProfile(CustomProviderProperties p)
	{
		if (p.isSet(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE))
			return TranslationProfileGenerator.getProfileFromString(
					p.getValue(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE));
		return TranslationProfileGenerator.generateIncludeInputProfile(
				p.getValue(CommonWebAuthnProperties.TRANSLATION_PROFILE));
	}

	private TranslationProfile parseFederationTranslationProfile(OAuthClientProperties props)
	{
		if (props.isSet(OAuthClientProperties.FEDERATION_EMBEDDED_TRANSLATION_PROFILE))
			return TranslationProfileGenerator.getProfileFromString(
					props.getValue(OAuthClientProperties.FEDERATION_EMBEDDED_TRANSLATION_PROFILE));
		if (props.isSet(OAuthClientProperties.FEDERATION_TRANSLATION_PROFILE))
			return TranslationProfileGenerator.generateIncludeInputProfile(
					props.getValue(OAuthClientProperties.FEDERATION_TRANSLATION_PROFILE));
		return TranslationProfileGenerator.generateIncludeInputProfile("sys:oidc");
	}
}
