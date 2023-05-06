/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.config;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.MetadataSignatureValidation;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;

@Component
public class SAMLSPConfigurationParser
{
	private final PKIManagement pkiMan;
	private final MessageSource msg;
	
	public SAMLSPConfigurationParser(@Qualifier("insecure") PKIManagement pkiMan,
			MessageSource msg)
	{
		this.pkiMan = pkiMan;
		this.msg = msg;
	}

	public SAMLSPConfiguration parse(Properties source)
	{
		SAMLSPProperties samlProperties = loadAsSamlSPProperties(source);
		return fromProperties(samlProperties);
	}
	
	public SAMLSPConfiguration parse(String source)
	{
		return parse(loadAsProperties(source));
	}

	private Properties loadAsProperties(String source)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			return properties;
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator(?)", e);
		}
	}
	
	private SAMLSPProperties loadAsSamlSPProperties(Properties properties)
	{
		try
		{
			return new SAMLSPProperties(properties, pkiMan);
		} catch (ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator", e);
		}
	}
	
	private SAMLSPConfiguration fromProperties(SAMLSPProperties samlProperties)
	{
		return SAMLSPConfiguration.builder()
				.withAcceptedNameFormats(samlProperties.getListOfValues(SAMLSPProperties.ACCEPTED_NAME_FORMATS))
				.withDefaultRequestedNameFormat(samlProperties.getValue(SAMLSPProperties.DEF_REQUESTED_NAME_FORMAT))
				.withEffectiveMappings(getEffectiveMappings(samlProperties))
				.withIndividualTrustedIdPs(getIndividualTrustedIdps(samlProperties))
				.withMetadataURLPath(samlProperties.getValue(SAMLSPProperties.METADATA_PATH))
				.withOurMetadataFilePath(samlProperties.getValue(SAMLSPProperties.METADATA_SOURCE))
				.withPublishMetadata(samlProperties.getBooleanValue(SAMLSPProperties.PUBLISH_METADATA))
				.withRequesterCredential(samlProperties.getRequesterCredential())
				.withRequesterCredentialName(samlProperties.getValue(SAMLSPProperties.CREDENTIAL))
				.withAlternativeRequesterCredentialName(samlProperties.getValue(SAMLSPProperties.ALTERNATIVE_CREDENTIAL))
				.withAlternativeRequesterCredential(samlProperties.getAlternativeRequesterCredential())
				.withRequesterSamlId(samlProperties.getValue(SAMLSPProperties.REQUESTER_ID))
				.withSignPublishedMetadata(samlProperties.getBooleanValue(SAMLSPProperties.SIGN_METADATA))
				.withSignRequestByDefault(samlProperties.getBooleanValue(SAMLSPProperties.DEF_SIGN_REQUEST))
				.withSloPath(samlProperties.getValue(SAMLSPProperties.SLO_PATH))
				.withSloRealm(samlProperties.getValue(SAMLSPProperties.SLO_REALM))
				.withRequireSignedAssertion(samlProperties.getBooleanValue(SAMLSPProperties.REQUIRE_SIGNED_ASSERTION))
				.withTrustedMetadataSources(getMetadataSources(samlProperties))
				.build();
	}
	
	private TrustedIdPs getIndividualTrustedIdps(SAMLSPProperties samlProperties)
	{
		Set<String> idpKeys = samlProperties.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		List<TrustedIdPConfiguration> trustedIdPs = idpKeys.stream()
			.filter(samlProperties::isIdPDefinitionComplete)
			.map(key -> getIndividualTrustedIdP(samlProperties, key))
			.collect(Collectors.toList());
		return new TrustedIdPs(trustedIdPs);
	}

	private TrustedIdPConfiguration getIndividualTrustedIdP(SAMLSPProperties samlProperties, String key)
	{
		boolean accountAssociationEnabled = samlProperties.isSet(key + CommonWebAuthnProperties.ENABLE_ASSOCIATION) ?
				samlProperties.getBooleanValue(key + CommonWebAuthnProperties.ENABLE_ASSOCIATION) :
				samlProperties.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION);
		return TrustedIdPConfiguration.builder()
				.withKey(TrustedIdPKey.individuallyConfigured(key))
				.withEnableAccountsAssocation(accountAssociationEnabled)
				.withSignRequest(samlProperties.isSignRequest(key))
				.withBinding(samlProperties.getEnumValue(
						key + SAMLSPProperties.IDP_BINDING, Binding.class))
				.withFederationId(samlProperties.getValue(
						key + SAMLSPProperties.IDP_FEDERATION_ID))
				.withFederationName(samlProperties.getValue(
						key + SAMLSPProperties.IDP_FEDERATION_NAME))
				.withCertificateNames(samlProperties.getCertificateNames(key))
				.withGroupMembershipAttribute(samlProperties.getValue(
						key + SAMLSPProperties.IDP_GROUP_MEMBERSHIP_ATTRIBUTE))
				.withIdpEndpointURL(samlProperties.getValue(
						key + SAMLSPProperties.IDP_ADDRESS))
				.withLogoURI(samlProperties.getLocalizedString(msg, key + SAMLSPProperties.IDP_LOGO))
				.withLogoutEndpoints(samlProperties.getLogoutEndpointsFromStructuredList(key))
				.withName(getIdpName(samlProperties, key))
				.withPublicKeys(samlProperties.getPublicKeysOfIdp(key))
				.withRegistrationForm(samlProperties.getValue(
						key + CommonWebAuthnProperties.REGISTRATION_FORM))
				.withRequestedNameFormat(samlProperties.getRequestedNameFormat(key))
				.withSamlId(samlProperties.getValue(key + SAMLSPProperties.IDP_ID))
				.withTags(Set.copyOf(samlProperties.getListOfValues(key + SAMLSPProperties.IDP_NAME + ".")))
				.withTranslationProfile(generateIndividualIdPTranslationProfile(samlProperties, key))
				.build();
	}

	private I18nString getIdpName(SAMLSPProperties samlProperties, String key)
	{
		boolean set = samlProperties.isSet(key + SAMLSPProperties.IDP_NAME);
		return set ? samlProperties.getLocalizedString(msg, key + SAMLSPProperties.IDP_NAME) : 
			new I18nString(samlProperties.getValue(key + SAMLSPProperties.IDP_ID));
	}
	
	private List<RemoteMetadataSource> getMetadataSources(SAMLSPProperties samlProperties)
	{
		Set<String> keys = samlProperties.getStructuredListKeys(SAMLSPProperties.IDPMETA_PREFIX);
		return keys.stream()
			.map(key -> 
				RemoteMetadataSource.builder()
					.withHttpsTruststore(samlProperties.getValue(
							key + SAMLSPProperties.METADATA_HTTPS_TRUSTSTORE))
					.withIssuerCertificate(samlProperties.getValue(
							key + SAMLSPProperties.METADATA_ISSUER_CERT))
					.withRefreshInterval(Duration.ofSeconds(samlProperties.getIntValue(
							key + SAMLSPProperties.METADATA_REFRESH)))
					.withRegistrationForm(samlProperties.getValue(
							key + SAMLSPProperties.IDPMETA_REGISTRATION_FORM))
					.withSignatureValidation(samlProperties.getEnumValue(
							key + SAMLSPProperties.METADATA_SIGNATURE, MetadataSignatureValidation.class))
					.withTranslationProfile(generateMetadataTranslationProfile(samlProperties, key))
					.withUrl(samlProperties.getValue(key + SAMLSPProperties.METADATA_URL))
					.withExcludedIdps(samlProperties.getListOfValues(key + SAMLSPProperties.IDPMETA_EXCLUDED_IDPS).stream().collect(Collectors.toSet()))
					.build())
			.collect(Collectors.toList());
	}

	private TranslationProfile generateMetadataTranslationProfile(SAMLSPProperties samlProperties, String key)
	{
		return generateTranslationProfile(samlProperties, key, 
				SAMLSPProperties.IDPMETA_EMBEDDED_TRANSLATION_PROFILE, 
				SAMLSPProperties.IDPMETA_TRANSLATION_PROFILE);
	}

	private TranslationProfile generateIndividualIdPTranslationProfile(SAMLSPProperties samlProperties, String key)
	{
		return generateTranslationProfile(samlProperties, key, 
				CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE, 
				CommonWebAuthnProperties.TRANSLATION_PROFILE);
	}

	private TranslationProfile generateTranslationProfile(SAMLSPProperties samlProperties, String key,
			String embeddedProfileProperty, String translationProfileProperty)
	{
		if (samlProperties.isSet(key + embeddedProfileProperty))
		{
			return TranslationProfileGenerator.getProfileFromString(samlProperties
					.getValue(key + embeddedProfileProperty));

		} else if (samlProperties.isSet(key + translationProfileProperty))
		{
			return TranslationProfileGenerator.generateIncludeInputProfile(
					samlProperties.getValue(key + translationProfileProperty));
		} else 
			return TranslationProfileGenerator.generateIncludeInputProfile("sys:saml");
	}

	
	private Map<String, String> getEffectiveMappings(SAMLSPProperties config)
	{
		Set<String> keys = config.getStructuredListKeys(SamlProperties.IDENTITY_MAPPING_PFX);
		Map<String, String> effectiveMappings = new HashMap<>(keys.size());
		effectiveMappings.putAll(IdentityTypeMapper.DEFAULTS);
		for (String key: keys)
		{
			String localId = config.getValue(key+SamlProperties.IDENTITY_LOCAL);
			String samlId = config.getValue(key+SamlProperties.IDENTITY_SAML);
			if (localId.trim().equals(""))
				effectiveMappings.remove(samlId);
			else
				effectiveMappings.put(samlId, localId);
		}
		return effectiveMappings;
	}
}
