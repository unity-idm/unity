/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.util.configuration.ConfigurationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.idp.*;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.MetadataSignatureValidation;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration.RemoteMetadataSource;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;

import java.io.IOException;
import java.io.StringReader;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static pl.edu.icm.unity.engine.api.idp.CommonIdPProperties.*;
import static pl.edu.icm.unity.saml.idp.SamlIdpProperties.*;

@Component
public class SAMLIdPConfigurationParser
{
	private final PKIManagement pkiMan;
	private final MessageSource msg;

	public SAMLIdPConfigurationParser(@Qualifier("insecure") PKIManagement pkiMan,
	                                  MessageSource msg)
	{
		this.pkiMan = pkiMan;
		this.msg = msg;
	}

	public SAMLIdPConfiguration parse(SamlIdpProperties source)
	{
		return fromProperties(source);
	}

	public SAMLIdPConfiguration parse(Properties source)
	{
		SamlIdpProperties samlProperties = loadAsSamlIdPProperties(source);
		return fromProperties(samlProperties);
	}
	
	public SAMLIdPConfiguration parse(String source)
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
	
	private SamlIdpProperties loadAsSamlIdPProperties(Properties properties)
	{
		try
		{
			return new SamlIdpProperties(properties);
		} catch (ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator", e);
		}
	}
	
	private SAMLIdPConfiguration fromProperties(SamlIdpProperties samlProperties)
	{
		return SAMLIdPConfiguration.builder()
				.withTrustedServiceProviders(getTrustedServiceProviders(samlProperties))
				.withIdentityTypeMapper(getEffectiveMappings(samlProperties))
				.withUserImportConfigs(getUserImportConfigs(samlProperties))
				.withOutputTranslationProfile(PropertiesTranslationProfileLoader
						.getTranslationProfile(samlProperties, TRANSLATION_PROFILE, EMBEDDED_TRANSLATION_PROFILE)
				)
				.withSkipConsent(samlProperties.getBooleanValue(CommonIdPProperties.SKIP_CONSENT))
				.withActiveValueClient(getActiveValueClients(samlProperties))
				.withPolicyAgreements(IdpPolicyAgreementsConfigurationParser.fromPropoerties(msg, samlProperties))
				.withChainValidator(getX509CertChainValidator(samlProperties))
				.withAuthenticationTimeout(samlProperties.getIntValue(SamlIdpProperties.AUTHENTICATION_TIMEOUT))
				.withSignResponses(samlProperties.getEnumValue(SamlIdpProperties.SIGN_RESPONSE, SAMLIdPConfiguration.ResponseSigningPolicy.class))
				.withSignAssertion(samlProperties.getEnumValue(SamlIdpProperties.SIGN_ASSERTION, SAMLIdPConfiguration.AssertionSigningPolicy.class))
				.withCredentialName(samlProperties.getValue(SamlIdpProperties.CREDENTIAL))
				.withAdditionallyAdvertisedCredential(getAdditionalyCredential(samlProperties))
				.withCredential(getSamlIssuerCredential(samlProperties.getValue(SamlIdpProperties.CREDENTIAL)))
				.withTruststore(samlProperties.getValue(SamlIdpProperties.TRUSTSTORE))
				.withValidityPeriod(Duration.of(samlProperties.getIntValue(SamlIdpProperties.DEF_ATTR_ASSERTION_VALIDITY), ChronoUnit.SECONDS))
				.withRequestValidityPeriod(Duration.of(samlProperties.getIntValue(SamlIdpProperties.SAML_REQUEST_VALIDITY), ChronoUnit.SECONDS))
				.withIssuerURI(samlProperties.getValue(SamlIdpProperties.ISSUER_URI))
				.withReturnSingleAssertion(samlProperties.getBooleanValue(SamlIdpProperties.RETURN_SINGLE_ASSERTION))
				.withSpAcceptPolicy(samlProperties.getEnumValue(SP_ACCEPT_POLICY, SAMLIdPConfiguration.RequestAcceptancePolicy.class))
				.withGroupChooser(getGroups(samlProperties), samlProperties.getValue(SamlIdpProperties.DEFAULT_GROUP))
				.withTrustedMetadataSources(getMetadataSources(samlProperties))
				.withUserCanEditConsent(samlProperties.getBooleanValue(SamlIdpProperties.USER_EDIT_CONSENT))
				.withPublishMetadata(samlProperties.getBooleanValue(SamlIdpProperties.PUBLISH_METADATA))
				.withMetadataURLPath(samlProperties.getValue(SamlIdpProperties.METADATA_URL))
				.withOurMetadataFilePath(samlProperties.getValue(SamlIdpProperties.METADATA_SOURCE))
				.withSignMetadata(samlProperties.getBooleanValue(SamlIdpProperties.SIGN_METADATA))
				.build();
	}

	private Optional<AdditionalyAdvertisedCredential> getAdditionalyCredential(SamlIdpProperties samlProperties)
	{
		String credName = samlProperties.getValue(SamlIdpProperties.ADDITIONALLY_ADVERTISED_CREDENTIAL);
		if (credName != null && !credName.isEmpty())
		{
			return Optional.of(new AdditionalyAdvertisedCredential(credName, getSamlIssuerCredential(credName)));
		}
		return Optional.empty();
	}
	
	private X509CertChainValidator getX509CertChainValidator(SamlIdpProperties samlProperties)
	{
		try
		{
			SAMLIdPConfiguration.RequestAcceptancePolicy policy = samlProperties.getEnumValue(SP_ACCEPT_POLICY, SAMLIdPConfiguration.RequestAcceptancePolicy.class);
			if (policy == SAMLIdPConfiguration.RequestAcceptancePolicy.validSigner)
			{
				String validator = samlProperties.getValue(TRUSTSTORE);
				if (validator == null)
					throw new ConfigurationException("The SAML truststore must be defined for " +
							"the selected SP acceptance policy " + policy);
				if (!pkiMan.getValidatorNames().contains(validator))
					throw new ConfigurationException("The SAML truststore " + validator + " is unknown");
				return pkiMan.getValidator(validator);
			}
			String credential = samlProperties.getValue(CREDENTIAL);
			if (!pkiMan.getCredentialNames().contains(credential))
				throw new ConfigurationException("The SAML credential " + credential + " is unknown");
			return null;
		} catch (EngineException e)
		{
			throw new RuntimeException(e);
		}
	}

	private X509Credential getSamlIssuerCredential(String credentialName)
	{
		try
		{
			return pkiMan.getCredential(credentialName);
		} catch (EngineException e)
		{
			throw new InternalException("Can't retrieve SAML credential", e);
		}
	}
	private TrustedServiceProviders getTrustedServiceProviders(SamlIdpProperties samlProperties)
	{
		Set<String> idpKeys = samlProperties.getStructuredListKeys(SamlIdpProperties.ALLOWED_SP_PREFIX);
		List<TrustedServiceProvider> trustedIdPs = idpKeys.stream()
			.map(key -> getTrustedSP(samlProperties, key))
			.collect(Collectors.toList());
		return new TrustedServiceProviders(trustedIdPs);
	}

	private UserImportConfigs getUserImportConfigs(SamlIdpProperties samlProperties)
	{
		Set<String> structuredListKeys = samlProperties.getStructuredListKeys(USERIMPORT_PFX);
		boolean skip = samlProperties.getBooleanValue(SKIP_USERIMPORT);
		if (structuredListKeys.isEmpty() || skip)
			return new UserImportConfigs(skip, Set.of());
		Set<UserImportConfig> configs = structuredListKeys.stream()
				.map(key -> new UserImportConfig(key, samlProperties.getValue(key + USERIMPORT_IMPORTER), samlProperties.getValue(key + USERIMPORT_IDENTITY_TYPE)))
				.collect(Collectors.toSet());
		return new UserImportConfigs(skip, configs);
	}

	private Set<ActiveValueClient> getActiveValueClients(SamlIdpProperties samlProperties)
	{
		Set<String> structuredListKeys = samlProperties.getStructuredListKeys(ACTIVE_VALUE_SELECTION_PFX);
		return structuredListKeys.stream()
				.map(key -> new ActiveValueClient(key, samlProperties.getValue(key + ACTIVE_VALUE_CLIENT),
						samlProperties.getListOfValues(key + ACTIVE_VALUE_SINGLE_SELECTABLE),
						samlProperties.getListOfValues(key + ACTIVE_VALUE_MULTI_SELECTABLE))
				).collect(Collectors.toSet());
	}

	private List<RemoteMetadataSource> getMetadataSources(SamlIdpProperties samlProperties)
	{
		Set<String> keys = samlProperties.getStructuredListKeys(SamlIdpProperties.SPMETA_PREFIX);
		return keys.stream().map(key -> RemoteMetadataSource.builder()
					.withUrl(samlProperties.getValue(key + SamlIdpProperties.METADATA_URL))
					.withHttpsTruststore(samlProperties.getValue(key + SamlIdpProperties.METADATA_HTTPS_TRUSTSTORE))
					.withIssuerCertificate(samlProperties.getValue(key + SamlIdpProperties.METADATA_ISSUER_CERT))
					.withRefreshInterval(Duration.ofSeconds(samlProperties.getIntValue(key + SamlIdpProperties.METADATA_REFRESH)))
					.withSignatureValidation(samlProperties.getEnumValue(key + SamlIdpProperties.METADATA_SIGNATURE, MetadataSignatureValidation.class))
					.withTranslationProfile(generateMetadataTranslationProfile(samlProperties, key))
					.build())
				.collect(Collectors.toList());
	}

	private Map<String, String> getGroups(SamlIdpProperties samlProperties)
	{
		Set<String> idpKeys = samlProperties.getStructuredListKeys(SamlIdpProperties.GROUP_PFX);
		return idpKeys.stream()
				.collect(Collectors.toMap(
						key -> samlProperties.getValue(key + SamlIdpProperties.GROUP),
						key -> samlProperties.getValue(key + SamlIdpProperties.GROUP_TARGET))
				);
	}

	private TrustedServiceProvider getTrustedSP(SamlIdpProperties samlProperties, String key)
	{
		return TrustedServiceProvider.builder()
				.withAllowedKey(key)
				.withDnSamlId(samlProperties.getValue(key + SamlIdpProperties.ALLOWED_SP_DN))
				.withEntityId(samlProperties.getValue(key + SamlIdpProperties.ALLOWED_SP_ENTITY))
				.withEncrypt(samlProperties.getBooleanValue(key + SamlIdpProperties.ALLOWED_SP_ENCRYPT))
				.withReturnUrl(samlProperties.getValue(key + SamlIdpProperties.ALLOWED_SP_RETURN_URL))
				.withReturnUrls(new HashSet<>(samlProperties.getListOfValues(key + SamlIdpProperties.ALLOWED_SP_RETURN_URLS)))
				.withSoapLogoutUrl(samlProperties.getValue(key + SamlIdpProperties.SOAP_LOGOUT_URL))
				.withRedirectLogoutUrl(samlProperties.getValue(key + SamlIdpProperties.REDIRECT_LOGOUT_URL))
				.withPostLogoutUrl(samlProperties.getValue(key + SamlIdpProperties.POST_LOGOUT_URL))
				.withRedirectLogoutRetUrl(samlProperties.getValue(key + SamlIdpProperties.REDIRECT_LOGOUT_RET_URL))
				.withPostLogoutRetUrl(samlProperties.getValue(key + SamlIdpProperties.POST_LOGOUT_RET_URL))
				.withName(getIdpName(samlProperties, key))
				.withLogoUri(getIdpLogoUrl(samlProperties, key))
				.withCertificate(getCertificate(key, samlProperties))
				.withCertificates(getCertificates(key, samlProperties))
				.withCertificateName(samlProperties.getValue(key + ALLOWED_SP_CERTIFICATE))
				.withCertificateNames(new HashSet<>(samlProperties.getListOfValues(key + SamlIdpProperties.ALLOWED_SP_CERTIFICATES)))
				.build();
	}


	private Set<X509Certificate> getCertificates(String key, SamlIdpProperties properties)
	{
		return properties.getListOfValues(key + SamlIdpProperties.ALLOWED_SP_CERTIFICATES).stream()
				.map(cert -> getCertificate(key, cert))
				.collect(Collectors.toSet());
	}

	private X509Certificate getCertificate(String key, SamlIdpProperties properties)
	{
		String value = properties.getValue(key + ALLOWED_SP_CERTIFICATE);
		if(value == null)
			return null;
		return getCertificate(key, value);
	}

	private X509Certificate getCertificate(String key, String name)
	{
		try
		{
			return pkiMan.getCertificate(name).value;
		} catch (EngineException e)
		{
			throw new InternalException("Can't retrieve SAML encryption certificate " + name +
					" for requester with config key " + key, e);
		}
	}

	private I18nString getIdpName(SamlIdpProperties samlProperties, String key)
	{
		boolean set = samlProperties.isSet(key + SamlIdpProperties.ALLOWED_SP_NAME);
		return set ? samlProperties.getLocalizedString(msg, key + SamlIdpProperties.ALLOWED_SP_NAME) : null;
	}

	private I18nString getIdpLogoUrl(SamlIdpProperties samlProperties, String key)
	{
		boolean set = samlProperties.isSet(key + SamlIdpProperties.ALLOWED_SP_LOGO);
		return set ? samlProperties.getLocalizedString(msg, key + SamlIdpProperties.ALLOWED_SP_LOGO) : null;
	}

	private TranslationProfile generateMetadataTranslationProfile(SamlIdpProperties samlProperties, String key)
	{
		return generateTranslationProfile(samlProperties, key, 
				SAMLSPProperties.IDPMETA_EMBEDDED_TRANSLATION_PROFILE, 
				SAMLSPProperties.IDPMETA_TRANSLATION_PROFILE);
	}

	private TranslationProfile generateIndividualIdPTranslationProfile(SamlIdpProperties samlProperties, String key)
	{
		return generateTranslationProfile(samlProperties, key, 
				CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE, 
				CommonWebAuthnProperties.TRANSLATION_PROFILE);
	}

	private TranslationProfile generateTranslationProfile(SamlIdpProperties samlProperties, String key,
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

	
	private Map<String, String> getEffectiveMappings(SamlIdpProperties config)
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
