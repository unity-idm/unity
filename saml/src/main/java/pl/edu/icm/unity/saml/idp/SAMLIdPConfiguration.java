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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SAMLIdPConfiguration extends BaseSamlConfiguration
{
	private static final Logger log = Log.getLogger(SamlIdpProperties.LOG_PFX, SAMLIdPConfiguration.class);

	public enum RequestAcceptancePolicy
	{
		all, validSigner, validRequester, strict
	}

	public enum ResponseSigningPolicy
	{
		always, never, asRequest
	}

	public enum AssertionSigningPolicy
	{
		always, ifResponseUnsigned
	}

	public final int authenticationTimeout;
	public final ResponseSigningPolicy signResponses;
	public final AssertionSigningPolicy signAssertion;
	public final String credentialName;
	public final String truststore;
	public final Duration validityPeriod;
	public final Duration requestValidityPeriod;
	public final String issuerURI;
	public final boolean returnSingleAssertion;
	public final RequestAcceptancePolicy spAcceptPolicy;
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

	public final Optional<AdditionalyAdvertisedCredential> additionallyAdvertisedCredential;

	SAMLIdPConfiguration(List<RemoteMetadataSource> trustedMetadataSources, boolean publishMetadata,
			String metadataURLPath, String ourMetadataFilePath, int authenticationTimeout,
			ResponseSigningPolicy signResponses, AssertionSigningPolicy signAssertion, String credentialName,
			String truststore, Duration validityPeriod, Duration requestValidityPeriod, String issuerURI,
			boolean returnSingleAssertion, RequestAcceptancePolicy spAcceptPolicy, boolean userCanEditConsent,
			TrustedServiceProviders trustedServiceProviders, GroupChooser groupChooser,
			IdentityTypeMapper identityTypeMapper, UserImportConfigs userImportConfigs,
			TranslationProfile translationProfile, boolean skipConsent, Set<ActiveValueClient> activeValueClient,
			IdpPolicyAgreementsConfiguration policyAgreements, X509Credential credential,
			X509CertChainValidator chainValidator, boolean signMetadata,
			Optional<AdditionalyAdvertisedCredential> additionalyAdvertisedCredential)
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
		this.activeValueClient = Set.copyOf(activeValueClient);
		this.policyAgreements = policyAgreements;
		this.credential = credential;
		this.trustedValidator = chainValidator;
		this.signMetadata = signMetadata;
		this.additionallyAdvertisedCredential = additionalyAdvertisedCredential;
		load();
	}

	public void load()
	{
		checkIssuer();
		ResponseSigningPolicy repPolicy = signResponses;
		signRespAlways = signRespNever = false;
		if (repPolicy == ResponseSigningPolicy.always)
			signRespAlways = true;
		else if (repPolicy == ResponseSigningPolicy.never)
			signRespNever = true;

		RequestAcceptancePolicy spPolicy = spAcceptPolicy;

		if (spPolicy == RequestAcceptancePolicy.all)
		{
			authnTrustChecker = new AcceptingSamlTrustChecker();
			log.info("All SPs will be authorized to submit authentication requests");
		} else if (spPolicy == RequestAcceptancePolicy.validSigner)
		{
			authnTrustChecker = new PKISamlTrustChecker(trustedValidator);
			log.info("All SPs using a valid certificate will be authorized to submit authentication requests");
		} else if (spPolicy == RequestAcceptancePolicy.strict)
		{
			authnTrustChecker = createStrictTrustChecker();
		} else
		{
			EnumeratedTrustChecker authnTrustChecker = new EnumeratedTrustChecker();
			this.authnTrustChecker = authnTrustChecker;
			initValidRequester(authnTrustChecker);
		}

		for (TrustedServiceProvider configuration : trustedServiceProviders.getSPConfigs())
		{
			if (configuration.encrypt && configuration.getCertificates()
					.isEmpty())
				throw new ConfigurationException(
						"Invalid specification of allowed Service " + "Provider " + configuration.allowedKey
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
			throw new ConfigurationException("SAML endpoint's issuer is not a valid URI: " + e.getMessage(), e);
		}
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

	public SamlAttributeMapper getAttributesMapper()
	{
		return attributesMapper;
	}

	public GroupChooser getGroupChooser()
	{
		return groupChooser;
	}

	private void initValidRequester(EnumeratedTrustChecker authnTrustChecker)
	{
		for (TrustedServiceProvider configuration : trustedServiceProviders.getSPConfigs())
		{
			String returnAddress = configuration.returnUrl;
			if (returnAddress == null)
				throw new ConfigurationException("Invalid specification of allowed Service " + "Provider "
						+ configuration.entityId + ", return address is not set.");

			if (configuration.entityId.id != null && configuration.entityId.dnSamlId != null)
				throw new ConfigurationException("The allowed SP entry " + configuration.allowedKey
						+ " has both the DN and SAML entity id defined. "
						+ "Please use only one, which is actually used by " + "the SP to identify itself.");

			String name = configuration.entityId.id;
			if (name != null)
			{
				Set<String> allowedEndpoints = configuration.returnUrls;
				allowedRequestersByIndex = initAllowedRequesters(allowedEndpoints);
				authnTrustChecker.addTrustedIssuer(name, returnAddress);
				for (String endpoint : allowedRequestersByIndex.values())
					authnTrustChecker.addTrustedIssuer(name, endpoint);
			} else
			{
				name = configuration.entityId.dnSamlId;
				if (name == null)
					throw new ConfigurationException("Invalid specification of allowed Service " + "Provider "
							+ configuration.allowedKey + ", neither Entity ID nor DN is set.");
				authnTrustChecker.addTrustedDNIssuer(name, returnAddress);
			}

			log.debug("SP authorized to submit authentication requests: " + name);
		}
	}

	public X509Certificate getEncryptionCertificateForRequester(NameIDType requester)
	{
		X509Certificate rc = null;
		TrustedServiceProvider config = getSPConfig(requester);
		if (config == null)
			return null;

		if (!config.encrypt)
			return null;

		for (X509Certificate c : config.getCertificates())
		{
			if (rc == null)
			{
				rc = c;
			} else if (c.getNotAfter()
					.compareTo(rc.getNotAfter()) > 0)
			{
				rc = c;
			}
		}
		return rc;
	}

	public void configureKnownRequesters(UnityAuthnRequestValidator validator)
	{
		for (TrustedServiceProvider configuration : trustedServiceProviders.getSPConfigs())
		{
			String name = configuration.entityId.id;
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

	public X509Credential getAdditionalyAdvertisedCredential()
	{
		return additionallyAdvertisedCredential.isPresent() ? additionallyAdvertisedCredential.get().credential : null;
	}

	static Map<Integer, String> initAllowedRequesters(Set<String> allowedEndpoints)
	{
		Map<Integer, String> allowedRequestersByIndex = new HashMap<>();
		Pattern pattern = Pattern.compile("\\[([\\d]+)\\](.+)");
		for (String endpoint : allowedEndpoints)
		{
			Matcher matcher = pattern.matcher(endpoint);
			if (!matcher.matches())
				throw new ConfigurationException(
						"SAML allowed endpoint '" + endpoint + "' has incorrect syntax. Should be [N]URL");
			String indexStr = matcher.group(1);
			String url = matcher.group(2);
			allowedRequestersByIndex.put(Integer.parseInt(indexStr), url);
		}
		return allowedRequestersByIndex;
	}

	private StrictSamlTrustChecker createStrictTrustChecker()
	{
		StrictSamlTrustChecker authnTrustChecker = new StrictSamlTrustChecker();
		for (TrustedServiceProvider configuration : trustedServiceProviders.getSPConfigs())
		{
			String type = SAMLConstants.NFORMAT_ENTITY;
			String name = configuration.entityId.id;
			if (name == null)
			{
				name = configuration.entityId.dnSamlId;
				type = SAMLConstants.NFORMAT_DN;
			}
			if (name == null)
				throw new ConfigurationException("Invalid specification of allowed Service " + "Provider "
						+ configuration.allowedKey + ", neither Entity ID nor DN is set.");

			for (X509Certificate spCert : configuration.getCertificates())
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
		TrustedServiceProvider config = getSPConfig(req.getIssuer());
		if (config == null)
			return null;
		Integer requestedServiceIdx = req.isSetAssertionConsumerServiceIndex() ? req.getAssertionConsumerServiceIndex()
				: null;
		return (requestedServiceIdx != null) ? allowedRequestersByIndex.get(requestedServiceIdx) : config.returnUrl;
	}

	public String getDisplayedNameForRequester(NameIDType id, MessageSource msg)
	{
		TrustedServiceProvider config = getSPConfig(id);
		if (config == null || config.name == null)
			return null;
		return config.name.getDefaultLocaleValue(msg);
	}

	public Resource getLogoForRequesterOrNull(NameIDType id, MessageSource msg, ImageAccessService imageAccessService)
	{
		TrustedServiceProvider config = getSPConfig(id);
		if (config == null || config.logoUri == null)
			return null;

		String logoURI = config.logoUri.getDefaultLocaleValue(msg);
		return imageAccessService.getConfiguredImageResourceFromNullableUri(logoURI)
				.orElse(null);
	}

	public TrustedServiceProvider getSPConfig(NameIDType requester)
	{
		boolean dnName = requester.getFormat() != null && requester.getFormat()
				.equals(SAMLConstants.NFORMAT_DN);
		for (TrustedServiceProvider configuration : trustedServiceProviders.getSPConfigs())
		{
			if (dnName)
			{
				String name = configuration.entityId.dnSamlId;
				if (name == null)
					continue;
				if (!X500NameUtils.equal(name, requester.getStringValue()))
					continue;
			} else
			{
				String name = configuration.entityId.id;
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
		TrustedServiceProvider configuration = getSPConfig(samlEntity);
		if (configuration == null)
			return null;
		return configuration.getCertificates()
				.stream()
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

	public Duration getAuthenticationTimeoutDuration()
	{
		return Duration.of(authenticationTimeout, ChronoUnit.SECONDS);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		SAMLIdPConfiguration that = (SAMLIdPConfiguration) o;
		return authenticationTimeout == that.authenticationTimeout && validityPeriod == that.validityPeriod
				&& requestValidityPeriod == that.requestValidityPeriod
				&& returnSingleAssertion == that.returnSingleAssertion && userCanEditConsent == that.userCanEditConsent
				&& skipConsent == that.skipConsent && signMetadata == that.signMetadata
				&& signRespNever == that.signRespNever && signRespAlways == that.signRespAlways
				&& signResponses == that.signResponses && signAssertion == that.signAssertion
				&& Objects.equals(credentialName, that.credentialName)
				&& Objects.equals(additionallyAdvertisedCredential, that.additionallyAdvertisedCredential)
				&& Objects.equals(truststore, that.truststore) && Objects.equals(issuerURI, that.issuerURI)
				&& spAcceptPolicy == that.spAcceptPolicy
				&& Objects.equals(trustedServiceProviders, that.trustedServiceProviders)
				&& Objects.equals(userImportConfigs, that.userImportConfigs)
				&& Objects.equals(translationProfile, that.translationProfile)
				&& Objects.equals(activeValueClient, that.activeValueClient)
				&& Objects.equals(policyAgreements, that.policyAgreements)
				&& Objects.equals(credential, that.credential)
				&& Objects.equals(trustedValidator, that.trustedValidator)
				&& Objects.equals(groupChooser, that.groupChooser)
				&& Objects.equals(attributesMapper, that.attributesMapper)
				&& Objects.equals(idTypeMapper, that.idTypeMapper) && Objects.equals(replayChecker, that.replayChecker)
				&& Objects.equals(authnTrustChecker, that.authnTrustChecker)
				&& Objects.equals(soapTrustChecker, that.soapTrustChecker)
				&& Objects.equals(allowedRequestersByIndex, that.allowedRequestersByIndex);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), authenticationTimeout, signResponses, signAssertion, credentialName,
				additionallyAdvertisedCredential, truststore, validityPeriod, requestValidityPeriod, issuerURI,
				returnSingleAssertion, spAcceptPolicy, userCanEditConsent, trustedServiceProviders, userImportConfigs,
				translationProfile, skipConsent, activeValueClient, policyAgreements, credential,
				trustedValidator, groupChooser, attributesMapper, idTypeMapper, signMetadata, signRespNever,
				signRespAlways, replayChecker, authnTrustChecker, soapTrustChecker, allowedRequestersByIndex);
	}

	@Override
	public String toString()
	{
		return "SAMLIdPConfiguration{" + "authenticationTimeout=" + authenticationTimeout + ", signResponses="
				+ signResponses + ", signAssertion=" + signAssertion + ", credentialName='" + credentialName + '\''
				+ ", truststore='" + truststore + '\'' + ", validityPeriod=" + validityPeriod
				+ ", requestValidityPeriod=" + requestValidityPeriod + ", issuerURI='" + issuerURI + '\''
				+ ", returnSingleAssertion=" + returnSingleAssertion + ", spAcceptPolicy=" + spAcceptPolicy
				+ ", userCanEditConsent=" + userCanEditConsent + ", trustedServiceProviders=" + trustedServiceProviders
				+ ", userImportConfigs=" + userImportConfigs + ", translationProfile=" + translationProfile
				+ ", skipConsent=" + skipConsent + ", activeValueClient=" + activeValueClient + ", policyAgreements="
				+ policyAgreements + ", credential=" + credential + ", trustedValidator=" + trustedValidator
				+ ", groupChooser=" + groupChooser + ", attributesMapper=" + attributesMapper + ", idTypeMapper="
				+ idTypeMapper + ", signMetadata=" + signMetadata + ", signRespNever=" + signRespNever
				+ ", signRespAlways=" + signRespAlways + ", replayChecker=" + replayChecker + ", authnTrustChecker="
				+ authnTrustChecker + ", soapTrustChecker=" + soapTrustChecker + ", allowedRequestersByIndex="
				+ allowedRequestersByIndex + '}';
	}

	public static SAMLIdPConfigurationBuilder builder()
	{
		return new SAMLIdPConfigurationBuilder();
	}

	public static final class SAMLIdPConfigurationBuilder
	{
		public int authenticationTimeout;
		public ResponseSigningPolicy signResponses;
		public AssertionSigningPolicy signAssertion;
		public String credentialName;
		public X509Credential credential;
		public String truststore;
		public Duration validityPeriod;
		public Duration requestValidityPeriod;
		public String issuerURI;
		public boolean returnSingleAssertion;
		public RequestAcceptancePolicy spAcceptPolicy;
		public List<RemoteMetadataSource> trustedMetadataSources = List.of();
		public boolean publishMetadata;
		public String metadataURLPath;
		public String ourMetadataFilePath;
		private boolean userCanEditConsent;
		private TrustedServiceProviders trustedServiceProviders = new TrustedServiceProviders(List.of());
		private GroupChooser groupChooser;
		private IdentityTypeMapper identityTypeMapper;
		private UserImportConfigs userImportConfigs;
		private TranslationProfile translationProfile;
		private boolean skipConsent;
		private Set<ActiveValueClient> activeValueClient = Set.of();
		private IdpPolicyAgreementsConfiguration policyAgreements;
		private X509CertChainValidator chainValidator;
		private boolean signMetadata;
		private Optional<AdditionalyAdvertisedCredential> additionallyAdvertisedCredential = Optional.empty();

		private SAMLIdPConfigurationBuilder()
		{
		}

		public SAMLIdPConfigurationBuilder withAuthenticationTimeout(int authenticationTimeout)
		{
			this.authenticationTimeout = authenticationTimeout;
			return this;
		}

		public SAMLIdPConfigurationBuilder withSignResponses(ResponseSigningPolicy signResponses)
		{
			this.signResponses = signResponses;
			return this;
		}

		public SAMLIdPConfigurationBuilder withSignAssertion(AssertionSigningPolicy signAssertion)
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

		public SAMLIdPConfigurationBuilder withAdditionallyAdvertisedCredential(
				Optional<AdditionalyAdvertisedCredential> additionalyAdvertisedCredential)
		{
			this.additionallyAdvertisedCredential = additionalyAdvertisedCredential;
			return this;
		}

		public SAMLIdPConfigurationBuilder withTruststore(String truststore)
		{
			this.truststore = truststore;
			return this;
		}

		public SAMLIdPConfigurationBuilder withValidityPeriod(Duration validityPeriod)
		{
			this.validityPeriod = validityPeriod;
			return this;
		}

		public SAMLIdPConfigurationBuilder withRequestValidityPeriod(Duration requestValidityPeriod)
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

		public SAMLIdPConfigurationBuilder withSpAcceptPolicy(RequestAcceptancePolicy spAcceptPolicy)
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

		public SAMLIdPConfigurationBuilder withTrustedMetadataSources(
				List<RemoteMetadataSource> trustedMetadataSourcesByUrl)
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
					userCanEditConsent, trustedServiceProviders, groupChooser, identityTypeMapper, userImportConfigs,
					translationProfile, skipConsent, activeValueClient, policyAgreements, credential, chainValidator,
					signMetadata, additionallyAdvertisedCredential);
		}
	}
}
