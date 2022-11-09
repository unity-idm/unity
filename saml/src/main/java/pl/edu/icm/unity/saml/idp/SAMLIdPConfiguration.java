/*
 * Copyright (c) 2022 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import com.vaadin.server.Resource;
import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.trust.*;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.util.configuration.ConfigurationException;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.idp.ActiveValueClient;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementsConfiguration;
import pl.edu.icm.unity.engine.api.idp.UserImportConfigs;
import pl.edu.icm.unity.saml.sp.config.BaseSamlConfiguration;
import pl.edu.icm.unity.saml.validator.UnityAuthnRequestValidator;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestType;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SAMLIdPConfiguration extends BaseSamlConfiguration
{
	private static final Logger log = Log.getLogger(SamlIdpProperties.LOG_PFX, SAMLIdPConfiguration.class);

	public final int authenticationTimeout;
	public final SamlIdpProperties.ResponseSigningPolicy signResponses;
	public final SamlIdpProperties.AssertionSigningPolicy signAssertion;
	public final String credentialName;
	public final String truststore;
	public final int validityPeriod;
	public final int requestValidityPeriod;
	public final String issuerURI;
	public final boolean returnSingleAssertion;
	public final SamlIdpProperties.RequestAcceptancePolicy spAcceptPolicy;
	public final boolean userCanEditConsent;
	public final TrustedServiceProviders trustedServiceProviders;
	public final UserImportConfigs userImportConfigs;
	public final TranslationProfile translationProfile;
	public final boolean skipConsent;
	public final Set<ActiveValueClient> activeValueClient;
	public final IdpPolicyAgreementsConfiguration policyAgreements;
	public final X509Credential credential;
	private final X509CertChainValidator trustedValidator;
	public final GroupChooser groupChooser;
	public final SamlAttributeMapper attributesMapper = new DefaultSamlAttributesMapper();
	public final IdentityTypeMapper idTypeMapper;

	public final boolean signMetadata;


	private boolean signRespNever;
	private boolean signRespAlways;
	private ReplayAttackChecker replayChecker;
	private SamlTrustChecker authnTrustChecker;
	private SamlTrustChecker soapTrustChecker;
	private Map<Integer, String> allowedRequestersByIndex;



	SAMLIdPConfiguration(List<RemoteMetadataSource> trustedMetadataSources, boolean publishMetadata, String metadataURLPath,
	                     String ourMetadataFilePath, int authenticationTimeout,
	                     SamlIdpProperties.ResponseSigningPolicy signResponses,
	                     SamlIdpProperties.AssertionSigningPolicy signAssertion, String credentialName, String truststore,
	                     int validityPeriod, int requestValidityPeriod, String issuerURI, boolean returnSingleAssertion,
	                     SamlIdpProperties.RequestAcceptancePolicy spAcceptPolicy,
	                     boolean userCanEditConsent, TrustedServiceProviders trustedServiceProviders,
	                     GroupChooser groupChooser, IdentityTypeMapper identityTypeMapper, UserImportConfigs userImportConfigs,
	                     TranslationProfile translationProfile, boolean skipConsent, Set<ActiveValueClient> activeValueClient,
	                     IdpPolicyAgreementsConfiguration policyAgreements, X509Credential credential,
	                     X509CertChainValidator chainValidator, boolean signMetadata)
	{
		super(trustedMetadataSources, publishMetadata, metadataURLPath, ourMetadataFilePath);
		this.authenticationTimeout = authenticationTimeout;
		this.signResponses = signResponses;
		this.signAssertion = signAssertion;
		this.credentialName = credentialName;
		this.truststore = truststore;
		this.validityPeriod = validityPeriod;
		this.requestValidityPeriod = requestValidityPeriod;
		this.issuerURI = issuerURI;
		this.returnSingleAssertion = returnSingleAssertion;
		this.spAcceptPolicy = spAcceptPolicy;
		this.userCanEditConsent = userCanEditConsent;
		this.trustedServiceProviders = trustedServiceProviders;
		this.groupChooser = groupChooser;
		this.idTypeMapper = identityTypeMapper;
		this.userImportConfigs = userImportConfigs;
		this.translationProfile = translationProfile;
		this.skipConsent = skipConsent;
		this.activeValueClient = activeValueClient;
		this.policyAgreements = policyAgreements;
		this.credential = credential;
		this.trustedValidator = chainValidator;
		this.signMetadata = signMetadata;
		init();
	}

	private void init()
	{
		checkIssuer();
		SamlIdpProperties.ResponseSigningPolicy repPolicy = signResponses;
		signRespAlways = signRespNever = false;
		if (repPolicy == SamlIdpProperties.ResponseSigningPolicy.always)
			signRespAlways = true;
		else if (repPolicy == SamlIdpProperties.ResponseSigningPolicy.never)
			signRespNever = true;

		SamlIdpProperties.RequestAcceptancePolicy spPolicy = spAcceptPolicy;

		if (spPolicy == SamlIdpProperties.RequestAcceptancePolicy.all)
		{
			authnTrustChecker = new AcceptingSamlTrustChecker();
			log.info("All SPs will be authorized to submit authentication requests");
		} else if (spPolicy == SamlIdpProperties.RequestAcceptancePolicy.validSigner)
		{
			authnTrustChecker = new PKISamlTrustChecker(trustedValidator);
			log.info("All SPs using a valid certificate will be authorized to submit authentication requests");
		} else if (spPolicy == SamlIdpProperties.RequestAcceptancePolicy.strict)
		{
			authnTrustChecker = createStrictTrustChecker();
		} else
		{
			EnumeratedTrustChecker authnTrustChecker = new EnumeratedTrustChecker();
			this.authnTrustChecker = authnTrustChecker;
			initValidRequester(authnTrustChecker);
		}

		for (TrustedServiceProviderConfiguration configuration: trustedServiceProviders.getSPConfigs())
		{
			if (configuration.encrypt && configuration.getCertificates().isEmpty())
				throw new ConfigurationException(
						"Invalid specification of allowed Service "
								+ "Provider "
								+ configuration.allowedKey
								+ " must have the certificate defined to be able to encrypt assertions.");
		}

		if (trustedValidator != null)
			soapTrustChecker = new PKISamlTrustChecker(trustedValidator, true);
		else
			soapTrustChecker = new AcceptingSamlTrustChecker();
		replayChecker = new ReplayAttackChecker();
	}

	private void checkIssuer()
	{
		try
		{
			new URI(issuerURI);
		} catch (URISyntaxException e)
		{
			throw new ConfigurationException("SAML endpoint's issuer is not a valid URI: " +
					e.getMessage(), e);
		}
	}

	public long getRequestValidity()
	{
		return requestValidityPeriod * 1000L;
	}

	public SamlTrustChecker getAuthnTrustChecker()
	{
		return authnTrustChecker;
	}

	public SamlTrustChecker getSoapTrustChecker()
	{
		return soapTrustChecker;
	}

	public ReplayAttackChecker getReplayChecker()
	{
		return replayChecker;
	}

	public TranslationProfile getOutputTranslationProfile()
	{
		return translationProfile;
	}
	private void initValidRequester(EnumeratedTrustChecker authnTrustChecker)
	{
		for (TrustedServiceProviderConfiguration configuration: trustedServiceProviders.getSPConfigs())
		{
			String returnAddress = configuration.returnUrl;
			if (returnAddress == null)
				throw new ConfigurationException("Invalid specification of allowed Service " +
						"Provider " + configuration.entityId + ", return address is not set.");

			if (configuration.entityId != null && configuration.dnSamlId != null)
				throw new ConfigurationException("The allowed SP entry " + configuration.allowedKey +
						" has both the DN and SAML entity id defined. "
						+ "Please use only one, which is actually used by "
						+ "the SP to identify itself." );

			String name = configuration.entityId;
			if (name != null)
			{
				Set<String> allowedEndpoints = configuration.returnUrls;
				allowedRequestersByIndex = initAllowedRequesters(allowedEndpoints);
				authnTrustChecker.addTrustedIssuer(name, returnAddress);
				for (String endpoint: allowedRequestersByIndex.values())
					authnTrustChecker.addTrustedIssuer(name, endpoint);
			} else
			{
				name = configuration.dnSamlId;
				if (name == null)
					throw new ConfigurationException("Invalid specification of allowed Service " +
							"Provider " + configuration.allowedKey + ", neither Entity ID nor DN is set.");
				authnTrustChecker.addTrustedDNIssuer(name, returnAddress);
			}

			log.debug("SP authorized to submit authentication requests: " + name);
		}
	}

	public X509Certificate getEncryptionCertificateForRequester(NameIDType requester)
	{
		X509Certificate rc = null;
		TrustedServiceProviderConfiguration config = getSPConfig(requester);
		if (config == null)
			return null;

		if (!config.encrypt)
			return null;

		for (X509Certificate c : config.getCertificates())
		{
			if (rc == null)
			{
				rc = c;
			} else if (c.getNotAfter().compareTo(rc.getNotAfter()) > 0)
			{
				rc = c;
			}
		}
		return rc;
	}

	public void configureKnownRequesters(UnityAuthnRequestValidator validator)
	{
		for (TrustedServiceProviderConfiguration configuration: trustedServiceProviders.getSPConfigs())
		{
			String name = configuration.entityId;
			if (name == null)
				continue;
			if (configuration.returnUrl == null)
				continue;
			validator.addKnownRequester(name);
		}
	}

	public X509Credential getSamlIssuerCredential()
	{
		return credential;
	}

	private Map<Integer, String> initAllowedRequesters(Set<String> allowedEndpoints)
	{
		Map<Integer, String> allowedRequestersByIndex = new HashMap<>();
		Pattern pattern = Pattern.compile("\\[([\\d]+)\\](.+)");
		for (String endpoint: allowedEndpoints)
		{
			Matcher matcher = pattern.matcher(endpoint);
			if (!matcher.matches())
				throw new ConfigurationException("SAML allowed endpoint '"
						+ endpoint + "' has incorrect syntax. Should be [N]URL");
			String indexStr = matcher.group(1);
			String url = matcher.group(2);
			allowedRequestersByIndex.put(Integer.parseInt(indexStr), url);
		}
		return allowedRequestersByIndex;
	}

	private StrictSamlTrustChecker createStrictTrustChecker()
	{
		StrictSamlTrustChecker authnTrustChecker = new StrictSamlTrustChecker();
		for (TrustedServiceProviderConfiguration configuration: trustedServiceProviders.getSPConfigs())
		{
			String type = SAMLConstants.NFORMAT_ENTITY;
			String name = configuration.entityId;
			if (name == null)
			{
				name = configuration.dnSamlId;
				type = SAMLConstants.NFORMAT_DN;
			}
			if (name == null)
				throw new ConfigurationException("Invalid specification of allowed Service " +
						"Provider " + configuration.allowedKey + ", neither Entity ID nor DN is set.");

			for (X509Certificate spCert: configuration.getCertificates())
			{
				authnTrustChecker.addTrustedIssuer(name, type, spCert.getPublicKey());
			}

			log.debug("SP authorized to submit authentication requests: " + name);
		}
		return authnTrustChecker;
	}

	public String getReturnAddressForRequester(AuthnRequestType req)
	{
		String requesterReturnUrl = req.getAssertionConsumerServiceURL();
		if (requesterReturnUrl != null)
			return requesterReturnUrl;
		TrustedServiceProviderConfiguration config = getSPConfig(req.getIssuer());
		if (config == null)
			return null;
		Integer requestedServiceIdx = req.isSetAssertionConsumerServiceIndex()
				? req.getAssertionConsumerServiceIndex()
				: null;
		return (requestedServiceIdx != null) ? allowedRequestersByIndex.get(requestedServiceIdx)
				: config.returnUrl;
	}

	public String getDisplayedNameForRequester(NameIDType id, MessageSource msg)
	{
		TrustedServiceProviderConfiguration config = getSPConfig(id);
		if (config == null)
			return null;
		return config.name.getDefaultLocaleValue(msg);
	}

	public Resource getLogoForRequesterOrNull(NameIDType id, MessageSource msg, ImageAccessService imageAccessService)
	{
		TrustedServiceProviderConfiguration config = getSPConfig(id);
		if (config == null)
			return null;

		String logoURI = config.logoUri.getDefaultLocaleValue(msg);
		return imageAccessService.getConfiguredImageResourceFromNullableUri(logoURI)
				.orElse(null);
	}

	public TrustedServiceProviderConfiguration getSPConfig(NameIDType requester)
	{
		boolean dnName = requester.getFormat() != null && requester.getFormat().equals(SAMLConstants.NFORMAT_DN);
		for (TrustedServiceProviderConfiguration configuration: trustedServiceProviders.getSPConfigs())
		{
			if (dnName)
			{
				String name = configuration.dnSamlId;
				if (name == null)
					continue;
				if (!X500NameUtils.equal(name, requester.getStringValue()))
					continue;
			} else
			{
				String name = configuration.entityId;
				if (name == null)
					continue;
				if (!name.equals(requester.getStringValue()))
					continue;
			}
			return configuration;
		}
		return null;
	}

	public List<PublicKey> getTrustedKeysForSamlEntity(NameIDType samlEntity)
	{
		TrustedServiceProviderConfiguration configuration = getSPConfig(samlEntity);
		if(configuration == null)
			return null;
		return configuration.getCertificates().stream()
				.map(Certificate::getPublicKey)
				.collect(Collectors.toList());
	}

	public boolean isSignRespNever()
	{
		return signRespNever;
	}

	public boolean isSignRespAlways()
	{
		return signRespAlways;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		SAMLIdPConfiguration that = (SAMLIdPConfiguration) o;
		return authenticationTimeout == that.authenticationTimeout &&
				validityPeriod == that.validityPeriod &&
				requestValidityPeriod == that.requestValidityPeriod &&
				returnSingleAssertion == that.returnSingleAssertion &&
				userCanEditConsent == that.userCanEditConsent &&
				skipConsent == that.skipConsent &&
				signMetadata == that.signMetadata &&
				signRespNever == that.signRespNever &&
				signRespAlways == that.signRespAlways &&
				signResponses == that.signResponses &&
				signAssertion == that.signAssertion &&
				Objects.equals(credentialName, that.credentialName) &&
				Objects.equals(truststore, that.truststore) &&
				Objects.equals(issuerURI, that.issuerURI) &&
				spAcceptPolicy == that.spAcceptPolicy &&
				Objects.equals(trustedServiceProviders, that.trustedServiceProviders) &&
				Objects.equals(userImportConfigs, that.userImportConfigs) &&
				Objects.equals(translationProfile, that.translationProfile) &&
				Objects.equals(activeValueClient, that.activeValueClient) &&
				Objects.equals(policyAgreements, that.policyAgreements) &&
				Objects.equals(credential, that.credential) &&
				Objects.equals(trustedValidator, that.trustedValidator) &&
				Objects.equals(groupChooser, that.groupChooser) &&
				Objects.equals(attributesMapper, that.attributesMapper) &&
				Objects.equals(idTypeMapper, that.idTypeMapper) &&
				Objects.equals(replayChecker, that.replayChecker) &&
				Objects.equals(authnTrustChecker, that.authnTrustChecker) &&
				Objects.equals(soapTrustChecker, that.soapTrustChecker) &&
				Objects.equals(allowedRequestersByIndex, that.allowedRequestersByIndex);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), authenticationTimeout, signResponses, signAssertion, credentialName,
				truststore, validityPeriod, requestValidityPeriod, issuerURI, returnSingleAssertion, spAcceptPolicy,
				userCanEditConsent, trustedServiceProviders, userImportConfigs, translationProfile, skipConsent,
				activeValueClient, policyAgreements, credential, trustedValidator, groupChooser, attributesMapper,
				idTypeMapper, signMetadata, signRespNever, signRespAlways, replayChecker, authnTrustChecker,
				soapTrustChecker, allowedRequestersByIndex);
	}

	@Override
	public String toString()
	{
		return "SAMLIdPConfiguration{" +
				"authenticationTimeout=" + authenticationTimeout +
				", signResponses=" + signResponses +
				", signAssertion=" + signAssertion +
				", credentialName='" + credentialName + '\'' +
				", truststore='" + truststore + '\'' +
				", validityPeriod=" + validityPeriod +
				", requestValidityPeriod=" + requestValidityPeriod +
				", issuerURI='" + issuerURI + '\'' +
				", returnSingleAssertion=" + returnSingleAssertion +
				", spAcceptPolicy=" + spAcceptPolicy +
				", userCanEditConsent=" + userCanEditConsent +
				", trustedServiceProviders=" + trustedServiceProviders +
				", userImportConfigs=" + userImportConfigs +
				", translationProfile=" + translationProfile +
				", skipConsent=" + skipConsent +
				", activeValueClient=" + activeValueClient +
				", policyAgreements=" + policyAgreements +
				", credential=" + credential +
				", trustedValidator=" + trustedValidator +
				", groupChooser=" + groupChooser +
				", attributesMapper=" + attributesMapper +
				", idTypeMapper=" + idTypeMapper +
				", signMetadata=" + signMetadata +
				", signRespNever=" + signRespNever +
				", signRespAlways=" + signRespAlways +
				", replayChecker=" + replayChecker +
				", authnTrustChecker=" + authnTrustChecker +
				", soapTrustChecker=" + soapTrustChecker +
				", allowedRequestersByIndex=" + allowedRequestersByIndex +
				'}';
	}

	public static SAMLIdPConfigurationBuilder builder()
	{
		return new SAMLIdPConfigurationBuilder();
	}

	public static final class SAMLIdPConfigurationBuilder
	{
		public int authenticationTimeout;
		public SamlIdpProperties.ResponseSigningPolicy signResponses;
		public SamlIdpProperties.AssertionSigningPolicy signAssertion;
		public String credentialName;
		public X509Credential credential;
		public String truststore;
		public int validityPeriod;
		public int requestValidityPeriod;
		public String issuerURI;
		public boolean returnSingleAssertion;
		public SamlIdpProperties.RequestAcceptancePolicy spAcceptPolicy;
		public List<RemoteMetadataSource> trustedMetadataSources;
		public boolean publishMetadata;
		public String metadataURLPath;
		public String ourMetadataFilePath;
		private boolean userCanEditConsent;
		private TrustedServiceProviders trustedServiceProviders;
		private GroupChooser groupChooser;
		private IdentityTypeMapper identityTypeMapper;
		private UserImportConfigs userImportConfigs;
		private TranslationProfile translationProfile;
		private boolean skipConsent;
		private Set<ActiveValueClient> activeValueClient;
		private IdpPolicyAgreementsConfiguration policyAgreements;
		private X509CertChainValidator chainValidator;
		private boolean signMetadata;

		private SAMLIdPConfigurationBuilder()
		{
		}

		public SAMLIdPConfigurationBuilder withAuthenticationTimeout(int authenticationTimeout)
		{
			this.authenticationTimeout = authenticationTimeout;
			return this;
		}

		public SAMLIdPConfigurationBuilder withSignResponses(SamlIdpProperties.ResponseSigningPolicy signResponses)
		{
			this.signResponses = signResponses;
			return this;
		}

		public SAMLIdPConfigurationBuilder withSignAssertion(SamlIdpProperties.AssertionSigningPolicy signAssertion)
		{
			this.signAssertion = signAssertion;
			return this;
		}

		public SAMLIdPConfigurationBuilder withCredentialName(String credentialName)
		{
			this.credentialName = credentialName;
			return this;
		}

		public SAMLIdPConfigurationBuilder withCredential(X509Credential credential)
		{
			this.credential = credential;
			return this;
		}

		public SAMLIdPConfigurationBuilder withTruststore(String truststore)
		{
			this.truststore = truststore;
			return this;
		}

		public SAMLIdPConfigurationBuilder withValidityPeriod(int validityPeriod)
		{
			this.validityPeriod = validityPeriod;
			return this;
		}

		public SAMLIdPConfigurationBuilder withRequestValidityPeriod(int requestValidityPeriod)
		{
			this.requestValidityPeriod = requestValidityPeriod;
			return this;
		}

		public SAMLIdPConfigurationBuilder withIssuerURI(String issuerURI)
		{
			this.issuerURI = issuerURI;
			return this;
		}

		public SAMLIdPConfigurationBuilder withReturnSingleAssertion(boolean returnSingleAssertion)
		{
			this.returnSingleAssertion = returnSingleAssertion;
			return this;
		}

		public SAMLIdPConfigurationBuilder withSpAcceptPolicy(SamlIdpProperties.RequestAcceptancePolicy spAcceptPolicy)
		{
			this.spAcceptPolicy = spAcceptPolicy;
			return this;
		}

		public SAMLIdPConfigurationBuilder withGroupChooser(Map<String, String> groupMappings, String defaultGroup)
		{
			this.groupChooser = new GroupChooser(groupMappings, defaultGroup);
			return this;
		}

		public SAMLIdPConfigurationBuilder withUserCanEditConsent(boolean userCanEditConsent)
		{
			this.userCanEditConsent = userCanEditConsent;
			return this;
		}

		public SAMLIdPConfigurationBuilder withTrustedServiceProviders(TrustedServiceProviders trustedServiceProviders)
		{
			this.trustedServiceProviders = trustedServiceProviders;
			return this;
		}

		public SAMLIdPConfigurationBuilder withIdentityTypeMapper(Map<String, String> samlIdToUnityId)
		{
			this.identityTypeMapper = new IdentityTypeMapper(samlIdToUnityId);
			return this;
		}

		public SAMLIdPConfigurationBuilder withTrustedMetadataSources(List<RemoteMetadataSource> trustedMetadataSourcesByUrl)
		{
			this.trustedMetadataSources = trustedMetadataSourcesByUrl;
			return this;
		}

		public SAMLIdPConfigurationBuilder withPublishMetadata(boolean publishMetadata)
		{
			this.publishMetadata = publishMetadata;
			return this;
		}

		public SAMLIdPConfigurationBuilder withMetadataURLPath(String metadataURLPath)
		{
			this.metadataURLPath = metadataURLPath;
			return this;
		}

		public SAMLIdPConfigurationBuilder withOurMetadataFilePath(String ourMetadataFilePath)
		{
			this.ourMetadataFilePath = ourMetadataFilePath;
			return this;
		}

		public SAMLIdPConfigurationBuilder withUserImportConfigs(UserImportConfigs userImportConfigs)
		{
			this.userImportConfigs = userImportConfigs;
			return this;
		}

		public SAMLIdPConfigurationBuilder withOutputTranslationProfile(TranslationProfile translationProfile)
		{
			this.translationProfile = translationProfile;
			return this;
		}

		public SAMLIdPConfigurationBuilder withSkipConsent(boolean skipConsent)
		{
			this.skipConsent = skipConsent;
			return this;
		}

		public SAMLIdPConfigurationBuilder withActiveValueClient(Set<ActiveValueClient> activeValueClient)
		{
			this.activeValueClient = activeValueClient;
			return this;
		}

		public SAMLIdPConfigurationBuilder withPolicyAgreements(IdpPolicyAgreementsConfiguration policyAgreements)
		{
			this.policyAgreements = policyAgreements;
			return this;
		}

		public SAMLIdPConfigurationBuilder withChainValidator(X509CertChainValidator chainValidator)
		{
			this.chainValidator = chainValidator;
			return this;
		}

		public SAMLIdPConfigurationBuilder withSignMetadata(boolean signMetadata)
		{
			this.signMetadata = signMetadata;
			return this;
		}

		public SAMLIdPConfiguration build()
		{
			return new SAMLIdPConfiguration(trustedMetadataSources, publishMetadata, metadataURLPath,
					ourMetadataFilePath, authenticationTimeout, signResponses, signAssertion, credentialName,
					truststore, validityPeriod, requestValidityPeriod, issuerURI, returnSingleAssertion, spAcceptPolicy,
					userCanEditConsent,
					trustedServiceProviders, groupChooser, identityTypeMapper, userImportConfigs, translationProfile,
					skipConsent, activeValueClient, policyAgreements, credential, chainValidator, signMetadata);
		}
	}
}
