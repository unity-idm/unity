/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console;

import com.vaadin.flow.server.StreamResource;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.services.idp.ActiveValueConfig;
import io.imunity.vaadin.auth.services.idp.GroupWithIndentIndicator;
import io.imunity.vaadin.endpoint.common.file.FileFieldUtils;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.api.idp.CommonIdPProperties;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementsConfiguration;
import pl.edu.icm.unity.engine.api.idp.IdpPolicyAgreementsConfigurationParser;
import pl.edu.icm.unity.engine.api.idp.UserImportConfig;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.console.SAMLIdentityMapping;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.AssertionSigningPolicy;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.RequestAcceptancePolicy;
import pl.edu.icm.unity.saml.idp.SAMLIdPConfiguration.ResponseSigningPolicy;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import io.imunity.vaadin.endpoint.common.VaadinEndpointProperties;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Related to {@link SamlIdpProperties}. Contains whole SAML service configuration
 * 
 * @author P.Piernik
 *
 */
public class SAMLServiceConfiguration
{
	private static Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLServiceConfiguration.class);

	private String issuerURI;
	private AssertionSigningPolicy signAssertionPolicy;
	private ResponseSigningPolicy signResponcePolicy;
	private String signResponseCredential;
	private String additionallyAdvertisedCredential;
	private String httpsTruststore;
	private boolean skipConsentScreen;
	private boolean editableConsentScreen;
	private RequestAcceptancePolicy requestAcceptancePolicy;
	private boolean publishMetadata;
	private boolean signMetadata;
	private boolean autoGenerateMetadata;
	private LocalOrRemoteResource metadataSource;
	private int authenticationTimeout;
	private int requestValidity;
	private int attrAssertionValidity;
	private boolean returnSingleAssertion;
	private List<SAMLIdentityMapping> identityMapping;
	private GroupWithIndentIndicator usersGroup;
	private List<ActiveValueConfig> activeValueSelections;
	private List<GroupMapping> groupMappings;
	private TranslationProfile translationProfile;
	private List<SAMLServiceTrustedFederationConfiguration> trustedFederations;
	private List<SAMLIndividualTrustedSPConfiguration> individualTrustedSPs;
	private List<UserImportConfig> userImports;
	private boolean skipUserImport;
	private IdpPolicyAgreementsConfiguration policyAgreementConfig;
	private boolean setNotBeforeConstraint;
 
	
	public SAMLServiceConfiguration(MessageSource msg, List<Group> allGroups)
	{
		Group root = allGroups.stream().filter(g -> g.toString().equals("/")).findAny().orElse(new Group("/"));
		usersGroup = new GroupWithIndentIndicator(root, false);
		translationProfile = TranslationProfileGenerator
				.generateIncludeOutputProfile(SamlIdpProperties.DEFAULT_TRANSLATION_PROFILE);

		signResponcePolicy = ResponseSigningPolicy.asRequest;
		signAssertionPolicy = AssertionSigningPolicy.always;
		skipConsentScreen = false;
		editableConsentScreen = true;
		requestAcceptancePolicy = RequestAcceptancePolicy.validRequester;
		authenticationTimeout = SamlIdpProperties.DEFAULT_AUTHENTICATION_TIMEOUT;
		requestValidity = SamlIdpProperties.DEFAULT_SAML_REQUEST_VALIDITY;
		attrAssertionValidity = SamlIdpProperties.DEFAULT_ATTR_ASSERTION_VALIDITY;
		returnSingleAssertion = true;
		identityMapping = new ArrayList<>();
		IdentityTypeMapper.DEFAULTS.entrySet().forEach(e -> {
			identityMapping.add(new SAMLIdentityMapping(e.getKey(), e.getValue()));
		});
		
		individualTrustedSPs = new ArrayList<>();
		trustedFederations = new ArrayList<>();
		publishMetadata = true;
		autoGenerateMetadata = true;
		userImports = new ArrayList<>();
		skipUserImport = false;
		policyAgreementConfig = new IdpPolicyAgreementsConfiguration(msg);
	}

	public String toProperties(PKIManagement pkiManagement, MessageSource msg, FileStorageService fileService,
			String name) throws ConfigurationException, IOException
	{
		Properties raw = new Properties();

		raw.put(SamlIdpProperties.P + SamlIdpProperties.ISSUER_URI, issuerURI);
		raw.put(SamlIdpProperties.P + SamlIdpProperties.SIGN_RESPONSE, signResponcePolicy.toString());
		raw.put(SamlIdpProperties.P + SamlIdpProperties.SIGN_ASSERTION, signAssertionPolicy.toString());
		raw.put(SamlIdpProperties.P + CommonIdPProperties.SKIP_USERIMPORT, String.valueOf(skipUserImport));
		
		if (signResponseCredential != null)
		{
			raw.put(SamlIdpProperties.P + SamlIdpProperties.CREDENTIAL, signResponseCredential);
		}

		if (getAdditionallyAdvertisedCredential() != null && !getAdditionallyAdvertisedCredential().isEmpty())
		{
			raw.put(SamlIdpProperties.P + SamlIdpProperties.ADDITIONALLY_ADVERTISED_CREDENTIAL, getAdditionallyAdvertisedCredential());
		}

		
		if (httpsTruststore != null)
		{
			raw.put(SamlIdpProperties.P + SamlIdpProperties.TRUSTSTORE, httpsTruststore);
		}

		raw.put(SamlIdpProperties.P + CommonIdPProperties.SKIP_CONSENT, String.valueOf(skipConsentScreen));
		raw.put(SamlIdpProperties.P + SamlIdpProperties.USER_EDIT_CONSENT,
				String.valueOf(editableConsentScreen));

		raw.put(SamlIdpProperties.P + SamlIdpProperties.SP_ACCEPT_POLICY,

				String.valueOf(requestAcceptancePolicy));

		raw.put(SamlIdpProperties.P + SamlIdpProperties.PUBLISH_METADATA, String.valueOf(publishMetadata));
		raw.put(SamlIdpProperties.P + SamlIdpProperties.SIGN_METADATA, String.valueOf(signMetadata));
		raw.put(SamlIdpProperties.P + SamlIdpProperties.SET_NOT_BEFORE_CONSTRAINT, String.valueOf(setNotBeforeConstraint));
		
		raw.put(SamlIdpProperties.P + SamlIdpProperties.AUTHENTICATION_TIMEOUT,
				String.valueOf(authenticationTimeout));

		raw.put(SamlIdpProperties.P + SamlIdpProperties.SAML_REQUEST_VALIDITY, String.valueOf(requestValidity));

		raw.put(SamlIdpProperties.P + SamlIdpProperties.DEF_ATTR_ASSERTION_VALIDITY,
				String.valueOf(attrAssertionValidity));

		raw.put(SamlIdpProperties.P + SamlIdpProperties.RETURN_SINGLE_ASSERTION,
				String.valueOf(returnSingleAssertion));

		if (identityMapping != null)
		{
			identityMapping.forEach(m -> {

				int i = identityMapping.indexOf(m) + 1;
				raw.put(SamlIdpProperties.P + SamlIdpProperties.IDENTITY_MAPPING_PFX + i + "."
						+ SamlIdpProperties.IDENTITY_LOCAL,
						m.getUnityId() == null ? "" : m.getUnityId());

				raw.put(SamlIdpProperties.P + SamlIdpProperties.IDENTITY_MAPPING_PFX + i + "."
						+ SamlIdpProperties.IDENTITY_SAML,
						m.getSamlId() == null ? "" : m.getSamlId());

			});
		}

		if (getMetadataSource() != null && !isAutoGenerateMetadata())
		{
			FileFieldUtils.saveInProperties(getMetadataSource(),
					SamlIdpProperties.P + SamlIdpProperties.METADATA_SOURCE, raw, fileService,
					StandardOwner.SERVICE.toString(), name);
		}

		try
		{
			raw.put(SamlIdpProperties.P + CommonIdPProperties.EMBEDDED_TRANSLATION_PROFILE,
					Constants.MAPPER.writeValueAsString(getTranslationProfile().toJsonObject()));
		} catch (Exception e)
		{
			throw new InternalException("Can't serialize oauth idp translation profile to JSON", e);
		}

		raw.put(SamlIdpProperties.P + SamlIdpProperties.DEFAULT_GROUP, usersGroup.group().toString());

		if (trustedFederations != null)
		{
			trustedFederations.stream().forEach(f -> f.toProperties(raw));
		}

		if (individualTrustedSPs != null)
		{
			individualTrustedSPs.stream().forEach(f -> f.toProperties(raw, msg, fileService, name));
		}

		if (activeValueSelections != null)
		{
			for (ActiveValueConfig acConfig : activeValueSelections)
			{
				String key = CommonIdPProperties.ACTIVE_VALUE_SELECTION_PFX
						+ (activeValueSelections.indexOf(acConfig) + 1) + ".";
				raw.put(SamlIdpProperties.P + key + CommonIdPProperties.ACTIVE_VALUE_CLIENT,
						acConfig.getClientId());

				List<String> sattributes = acConfig.getSingleSelectableAttributes();
				if (sattributes != null)
				{
					for (String attr : sattributes)
					{
						raw.put(SamlIdpProperties.P + key
								+ CommonIdPProperties.ACTIVE_VALUE_SINGLE_SELECTABLE
								+ (sattributes.indexOf(attr) + 1), attr);
					}
				}

				List<String> mattributes = acConfig.getMultiSelectableAttributes();
				if (mattributes != null)
				{
					for (String attr : mattributes)
					{
						raw.put(SamlIdpProperties.P + key
								+ CommonIdPProperties.ACTIVE_VALUE_MULTI_SELECTABLE
								+ (mattributes.indexOf(attr) + 1), attr);
					}
				}

			}
		}

		if (groupMappings != null)
		{
			for (GroupMapping mapping : groupMappings)
			{
				String key = SamlIdpProperties.GROUP_PFX + (groupMappings.indexOf(mapping) + 1) + ".";

				raw.put(SamlIdpProperties.P + key + SamlIdpProperties.GROUP_TARGET,
						mapping.getClientId());
				raw.put(SamlIdpProperties.P + key + SamlIdpProperties.GROUP,
						mapping.getGroup().group().toString());
			}
		}
		
		if (userImports != null)
		{
			for (UserImportConfig impConfig : userImports)
			{
				String key = CommonIdPProperties.USERIMPORT_PFX
						+ (userImports.indexOf(impConfig) + 1) + ".";
				raw.put(SamlIdpProperties.P + key + CommonIdPProperties.USERIMPORT_IMPORTER,
						impConfig.importer);
				raw.put(SamlIdpProperties.P + key + CommonIdPProperties.USERIMPORT_IDENTITY_TYPE,
						impConfig.type);
			}
		}
		
		if (policyAgreementConfig != null)
		{
			raw.putAll(IdpPolicyAgreementsConfigurationParser.toProperties(msg, policyAgreementConfig, SamlIdpProperties.P));
		}

		SamlIdpProperties samlProperties = new SamlIdpProperties(raw);
		return samlProperties.getAsString();
	}

	public void fromProperties(String properties, MessageSource msg, URIAccessService uriAccessService,
			VaadinLogoImageLoader imageAccessService,
			List<Group> allGroups) throws ConfigurationException
	{
		Properties raw = new Properties();

		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the SAML idp service", e);
		}
		new VaadinEndpointProperties(raw);
		
		SamlIdpProperties samlIdpProperties = new SamlIdpProperties(raw);
		issuerURI = samlIdpProperties.getValue(SamlIdpProperties.ISSUER_URI);

		signResponcePolicy = samlIdpProperties.getEnumValue(SamlIdpProperties.SIGN_RESPONSE,
				ResponseSigningPolicy.class);

		signAssertionPolicy = samlIdpProperties.getEnumValue(SamlIdpProperties.SIGN_ASSERTION,
				AssertionSigningPolicy.class);

		signResponseCredential = samlIdpProperties.getValue(SamlIdpProperties.CREDENTIAL);
		setAdditionallyAdvertisedCredential(samlIdpProperties.getValue(SamlIdpProperties.ADDITIONALLY_ADVERTISED_CREDENTIAL));

		httpsTruststore = samlIdpProperties.getValue(SamlIdpProperties.TRUSTSTORE);

		skipConsentScreen = samlIdpProperties.getBooleanValue(CommonIdPProperties.SKIP_CONSENT);
		editableConsentScreen = samlIdpProperties.getBooleanValue(SamlIdpProperties.USER_EDIT_CONSENT);

		requestAcceptancePolicy = samlIdpProperties.getEnumValue(SamlIdpProperties.SP_ACCEPT_POLICY,
				RequestAcceptancePolicy.class);

		if (samlIdpProperties.isSet(SamlProperties.PUBLISH_METADATA))
		{
			publishMetadata = samlIdpProperties.getBooleanValue(SamlProperties.PUBLISH_METADATA);
		}

		if (samlIdpProperties.isSet(SamlProperties.SIGN_METADATA))
		{
			signMetadata = samlIdpProperties.getBooleanValue(SamlProperties.SIGN_METADATA);
		}

		if (samlIdpProperties.isSet(SamlIdpProperties.SET_NOT_BEFORE_CONSTRAINT))
		{
			setNotBeforeConstraint = samlIdpProperties.getBooleanValue(SamlIdpProperties.SET_NOT_BEFORE_CONSTRAINT);
		}

		
		if (samlIdpProperties.isSet(SamlProperties.METADATA_SOURCE))
		{
			autoGenerateMetadata = false;

			String metaUri = samlIdpProperties.getValue(SamlProperties.METADATA_SOURCE);

			try
			{
				URI uri = URIHelper.parseURI(metaUri);
				if (URIHelper.isWebReady(uri))
				{
					metadataSource = new LocalOrRemoteResource(uri.toString(), "");
				} else
				{
					FileData fileData = uriAccessService.readURI(uri);
					metadataSource = new LocalOrRemoteResource(new StreamResource("metadata",
							() -> new ByteArrayInputStream(fileData.getContents())),
							uri.toString(), fileData.getContents());
				}

			} catch (Exception e)
			{
				log.error("Can not load configured metadata from uri: " + metaUri);
			}
		} else
		{
			autoGenerateMetadata = true;
		}

		authenticationTimeout = samlIdpProperties.getIntValue(SamlIdpProperties.AUTHENTICATION_TIMEOUT);
		requestValidity = samlIdpProperties.getIntValue(SamlIdpProperties.SAML_REQUEST_VALIDITY);
		attrAssertionValidity = samlIdpProperties.getIntValue(SamlIdpProperties.DEF_ATTR_ASSERTION_VALIDITY);

		returnSingleAssertion = samlIdpProperties.getBooleanValue(SamlIdpProperties.RETURN_SINGLE_ASSERTION);

		Set<String> identityMappingKeys = samlIdpProperties
				.getStructuredListKeys(SamlIdpProperties.IDENTITY_MAPPING_PFX);

		identityMapping = new ArrayList<>();
		identityMappingKeys.forEach(

				key -> {
					SAMLIdentityMapping m = new SAMLIdentityMapping();
					if (samlIdpProperties.getValue(key + SamlIdpProperties.IDENTITY_LOCAL) != null
							&& !samlIdpProperties.getValue(
									key + SamlIdpProperties.IDENTITY_LOCAL)
									.isEmpty())
					{
						m.setUnityId(samlIdpProperties
								.getValue(key + SamlIdpProperties.IDENTITY_LOCAL));
					}

					if (samlIdpProperties.getValue(key + SamlIdpProperties.IDENTITY_SAML) != null
							&& !samlIdpProperties
									.getValue(key + SamlIdpProperties.IDENTITY_SAML)
									.isEmpty())
					{
						m.setSamlId(samlIdpProperties
								.getValue(key + SamlIdpProperties.IDENTITY_SAML));
					}
					identityMapping.add(m);
				});

		if (samlIdpProperties.isSet(CommonIdPProperties.EMBEDDED_TRANSLATION_PROFILE))
		{
			translationProfile = TranslationProfileGenerator.getProfileFromString(
					samlIdpProperties.getValue(CommonIdPProperties.EMBEDDED_TRANSLATION_PROFILE));

		} else if (samlIdpProperties.getValue(CommonIdPProperties.TRANSLATION_PROFILE) != null)
		{
			translationProfile = TranslationProfileGenerator.generateIncludeOutputProfile(
					samlIdpProperties.getValue(CommonIdPProperties.TRANSLATION_PROFILE));
		} else
		{
			translationProfile = TranslationProfileGenerator.generateIncludeOutputProfile(
					SamlIdpProperties.DEFAULT_TRANSLATION_PROFILE);
		}

		String usersGroupPath = samlIdpProperties.getValue(SamlIdpProperties.DEFAULT_GROUP);
		usersGroup =

				new GroupWithIndentIndicator(
						allGroups.stream().filter(g -> g.toString().equals(usersGroupPath))
								.findFirst().orElse(new Group(usersGroupPath)),
						false);

		Set<String> fedKeys = samlIdpProperties.getStructuredListKeys(SamlIdpProperties.SPMETA_PREFIX);
		trustedFederations = new ArrayList<>();
		fedKeys.forEach(

				key -> {
					SAMLServiceTrustedFederationConfiguration fed = new SAMLServiceTrustedFederationConfiguration();
					key = key.substring(SamlIdpProperties.SPMETA_PREFIX.length(), key.length() - 1);
					fed.fromProperties(samlIdpProperties, key);
					trustedFederations.add(fed);
				});

		Set<String> spKeys = samlIdpProperties.getStructuredListKeys(SamlIdpProperties.ALLOWED_SP_PREFIX);

		individualTrustedSPs = new ArrayList<>();
		spKeys.forEach(

				key -> {
					SAMLIndividualTrustedSPConfiguration idp = new SAMLIndividualTrustedSPConfiguration();
					key = key.substring(SamlIdpProperties.ALLOWED_SP_PREFIX.length(),
							key.length() - 1);
					idp.fromProperties(msg, imageAccessService, samlIdpProperties, key);
					individualTrustedSPs.add(idp);
				});

		activeValueSelections = new ArrayList<>();

		Set<String> attrKeys = samlIdpProperties
				.getStructuredListKeys(CommonIdPProperties.ACTIVE_VALUE_SELECTION_PFX);

		for (String attrKey : attrKeys)
		{
			String id = samlIdpProperties.getValue(attrKey + CommonIdPProperties.ACTIVE_VALUE_CLIENT);
			List<String> sattrs = samlIdpProperties
					.getListOfValues(attrKey + CommonIdPProperties.ACTIVE_VALUE_SINGLE_SELECTABLE);
			List<String> mattrs = samlIdpProperties
					.getListOfValues(attrKey + CommonIdPProperties.ACTIVE_VALUE_MULTI_SELECTABLE);
			ActiveValueConfig ativeValConfig = new ActiveValueConfig();
			ativeValConfig.setClientId(id);
			ativeValConfig.setSingleSelectableAttributes(sattrs);
			ativeValConfig.setMultiSelectableAttributes(mattrs);
			activeValueSelections.add(ativeValConfig);
		}

		Set<String> groupMappingsKeys = samlIdpProperties.getStructuredListKeys(SamlIdpProperties.GROUP_PFX);
		groupMappings = new ArrayList<>();
		groupMappingsKeys.forEach(

				key -> {
					GroupMapping mapping = new GroupMapping();
					if (samlIdpProperties.getValue(key + SamlIdpProperties.GROUP_TARGET) != null
							&& !samlIdpProperties
									.getValue(key + SamlIdpProperties.GROUP_TARGET)
									.isEmpty())
					{
						mapping.setClientId(samlIdpProperties
								.getValue(key + SamlIdpProperties.GROUP_TARGET));
					}

					if (samlIdpProperties.getValue(key + SamlIdpProperties.GROUP) != null
							&& !samlIdpProperties.getValue(key + SamlIdpProperties.GROUP)
									.isEmpty())
					{

						String group = samlIdpProperties
								.getValue(key + SamlIdpProperties.GROUP);
						mapping.setGroup(new GroupWithIndentIndicator(
								allGroups.stream()
										.filter(g -> g.toString().equals(group))
										.findFirst().orElse(new Group(group)),
								false));
					}
					groupMappings.add(mapping);
				});
		
		skipUserImport = samlIdpProperties.getBooleanValue(CommonIdPProperties.SKIP_USERIMPORT);
		
		Set<String> importKeys = samlIdpProperties.getStructuredListKeys(CommonIdPProperties.USERIMPORT_PFX);

		for (String importKey : importKeys)
		{
			String importer = samlIdpProperties.getValue(importKey + CommonIdPProperties.USERIMPORT_IMPORTER);
			String identityType = samlIdpProperties
					.getValue(importKey + CommonIdPProperties.USERIMPORT_IDENTITY_TYPE);

			UserImportConfig userImportConfig = new UserImportConfig(null, importer, identityType);
			userImports.add(userImportConfig);
		}
		
		policyAgreementConfig = IdpPolicyAgreementsConfigurationParser.fromPropoerties(msg, samlIdpProperties);
	}

	public GroupWithIndentIndicator getUsersGroup()
	{
		return usersGroup;
	}

	public void setUsersGroup(GroupWithIndentIndicator usersGroup)
	{
		this.usersGroup = usersGroup;
	}

	public List<ActiveValueConfig> getActiveValueSelections()
	{
		return activeValueSelections;
	}

	public void setActiveValueSelections(List<ActiveValueConfig> activeValueSelections)
	{
		this.activeValueSelections = activeValueSelections;
	}

	public String getIssuerURI()
	{
		return issuerURI;
	}

	public void setIssuerURI(String issuerURI)
	{
		this.issuerURI = issuerURI;
	}

	public TranslationProfile getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(TranslationProfile translationProfile)
	{
		this.translationProfile = translationProfile;
	}

	public ResponseSigningPolicy getSignResponcePolicy()
	{
		return signResponcePolicy;
	}

	public void setSignResponcePolicy(ResponseSigningPolicy signResponcePolicy)
	{
		this.signResponcePolicy = signResponcePolicy;
	}

	public AssertionSigningPolicy getSignAssertionPolicy()
	{
		return signAssertionPolicy;
	}

	public void setSignAssertionPolicy(AssertionSigningPolicy signAssertionPolicy)
	{
		this.signAssertionPolicy = signAssertionPolicy;
	}

	public String getSignResponseCredential()
	{
		return signResponseCredential;
	}

	public void setSignResponseCredential(String signResponseCredential)
	{
		this.signResponseCredential = signResponseCredential;
	}

	public String getHttpsTruststore()
	{
		return httpsTruststore;
	}

	public void setHttpsTruststore(String httpsTruststore)
	{
		this.httpsTruststore = httpsTruststore;
	}

	public boolean isSkipConsentScreen()
	{
		return skipConsentScreen;
	}

	public void setSkipConsentScreen(boolean skipConsentScreen)
	{
		this.skipConsentScreen = skipConsentScreen;
	}

	public boolean isEditableConsentScreen()
	{
		return editableConsentScreen;
	}

	public void setEditableConsentScreen(boolean editableConsentScreen)
	{
		this.editableConsentScreen = editableConsentScreen;
	}

	public RequestAcceptancePolicy getRequestAcceptancePolicy()
	{
		return requestAcceptancePolicy;
	}

	public void setRequestAcceptancePolicy(RequestAcceptancePolicy requestAcceptancePolicy)
	{
		this.requestAcceptancePolicy = requestAcceptancePolicy;
	}

	public boolean isPublishMetadata()
	{
		return publishMetadata;
	}

	public void setPublishMetadata(boolean publishMetadata)
	{
		this.publishMetadata = publishMetadata;
	}

	public boolean isSignMetadata()
	{
		return signMetadata;
	}

	public void setSignMetadata(boolean signMetadata)
	{
		this.signMetadata = signMetadata;
	}

	public boolean isAutoGenerateMetadata()
	{
		return autoGenerateMetadata;
	}

	public void setAutoGenerateMetadata(boolean autoGenerateMetadata)
	{
		this.autoGenerateMetadata = autoGenerateMetadata;
	}

	public LocalOrRemoteResource getMetadataSource()
	{
		return metadataSource;
	}

	public void setMetadataSource(LocalOrRemoteResource metadataSource)
	{
		this.metadataSource = metadataSource;
	}

	public int getAuthenticationTimeout()
	{
		return authenticationTimeout;
	}

	public void setAuthenticationTimeout(int authenticationTimeout)
	{
		this.authenticationTimeout = authenticationTimeout;
	}

	public int getRequestValidity()
	{
		return requestValidity;
	}

	public void setRequestValidity(int requestValidity)
	{
		this.requestValidity = requestValidity;
	}

	public int getAttrAssertionValidity()
	{
		return attrAssertionValidity;
	}

	public void setAttrAssertionValidity(int attrAssertionValidity)
	{
		this.attrAssertionValidity = attrAssertionValidity;
	}

	public boolean isReturnSingleAssertion()
	{
		return returnSingleAssertion;
	}

	public void setReturnSingleAssertion(boolean returnSingleAssertion)
	{
		this.returnSingleAssertion = returnSingleAssertion;
	}

	public List<SAMLIdentityMapping> getIdentityMapping()
	{
		return identityMapping;
	}

	public void setIdentityMapping(List<SAMLIdentityMapping> identityMapping)
	{
		this.identityMapping = identityMapping;
	}

	public List<SAMLServiceTrustedFederationConfiguration> getTrustedFederations()
	{
		return trustedFederations;
	}

	public void setTrustedFederations(List<SAMLServiceTrustedFederationConfiguration> trustedFederations)
	{
		this.trustedFederations = trustedFederations;
	}

	public List<SAMLIndividualTrustedSPConfiguration> getIndividualTrustedSPs()
	{
		return individualTrustedSPs;
	}

	public void setIndividualTrustedSPs(List<SAMLIndividualTrustedSPConfiguration> individualTrustedSPs)
	{
		this.individualTrustedSPs = individualTrustedSPs;
	}

	public List<GroupMapping> getGroupMappings()
	{
		return groupMappings;
	}

	public void setGroupMappings(List<GroupMapping> groupMappings)
	{
		this.groupMappings = groupMappings;
	}

	public IdpPolicyAgreementsConfiguration getPolicyAgreementConfig()
	{
		return policyAgreementConfig;
	}

	public void setPolicyAgreementConfig(IdpPolicyAgreementsConfiguration policyAgreementConfig)
	{
		this.policyAgreementConfig = policyAgreementConfig;
	}

	public String getAdditionallyAdvertisedCredential()
	{
		return additionallyAdvertisedCredential;
	}

	public void setAdditionallyAdvertisedCredential(String additionallyAdvertisedCredential)
	{
		this.additionallyAdvertisedCredential = additionallyAdvertisedCredential;
	}

	public boolean isSetNotBeforeConstraint()
	{
		return setNotBeforeConstraint;
	}

	public void setSetNotBeforeConstraint(boolean sendNotBeforeConstraint)
	{
		this.setNotBeforeConstraint = sendNotBeforeConstraint;
	}

	
}
