/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.console.v8;

import eu.unicore.util.configuration.ConfigurationException;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.console.SAMLIdentityMapping;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.*;

/**
 * Related to {@link SAMLSPProperties}. Contains whole SAML authenticator
 * configuration
 * 
 * @author P.Piernik
 *
 */
public class SAMLAuthneticatorConfiguration
{
	private static Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLAuthneticatorConfiguration.class);

	private String requesterId;
	private String credential;
	private String additionalCredential;
	private boolean includeAddtionalCredentialInMetadata;
	private List<String> acceptedNameFormats;
	private boolean requireSignedAssertion;
	private boolean defSignRequest;
	private List<String> defaultRequestedNameFormat;
	private boolean defAccountAssociation;
	private List<SAMLAuthnTrustedFederationConfiguration> trustedFederations;
	private List<SAMLIndividualTrustedSamlIdpConfiguration> individualTrustedIdps;
	private boolean publishMetadata;
	private String metadataPath;
	private boolean signMetadata;
	private boolean autoGenerateMetadata;
	private LocalOrRemoteResource metadataSource;
	private String sloPath;
	private String sloRealm;
	private List<SAMLIdentityMapping> sloMappings;

	public SAMLAuthneticatorConfiguration()
	{
		setPublishMetadata(true);
		setAutoGenerateMetadata(true);
		setMetadataPath("sp");
		setDefAccountAssociation(true);
	}

	public String toProperties(PKIManagement pkiMan, FileStorageService fileService, MessageSource msg,
			String name) throws ConfigurationException
	{
		Properties raw = new Properties();
		raw.put(SAMLSPProperties.P + SAMLSPProperties.REQUESTER_ID, getRequesterId());

		if (getCredential() != null)
		{
			raw.put(SAMLSPProperties.P + SAMLSPProperties.CREDENTIAL, getCredential());
		}
		
		if (getAdditionalCredential() != null && !getAdditionalCredential().isEmpty())
		{
			raw.put(SAMLSPProperties.P + SAMLSPProperties.ADDITIONAL_CREDENTIAL, getAdditionalCredential());
		}

		raw.put(SAMLSPProperties.P + SAMLSPProperties.INCLUDE_ADDITIONAL_CREDENTIAL_IN_METADATA, String.valueOf(includeAddtionalCredentialInMetadata));
		
		if (acceptedNameFormats != null)
		{
			acceptedNameFormats.stream().forEach(
					f -> raw.put(SAMLSPProperties.P + SAMLSPProperties.ACCEPTED_NAME_FORMATS
							+ (acceptedNameFormats.indexOf(f) + 1), f));
		}

		raw.put(SAMLSPProperties.P + SAMLSPProperties.REQUIRE_SIGNED_ASSERTION,
				String.valueOf(isRequireSignedAssertion()));
		raw.put(SAMLSPProperties.P + SAMLSPProperties.DEF_SIGN_REQUEST, String.valueOf(isDefSignRequest()));

		if (defaultRequestedNameFormat != null)
		{
			defaultRequestedNameFormat.stream().forEach(d -> raw
					.put(SAMLSPProperties.P + SAMLSPProperties.DEF_REQUESTED_NAME_FORMAT, d));
		}

		raw.put(SAMLSPProperties.P + CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION,
				String.valueOf(isDefAccountAssociation()));

		if (getTrustedFederations() != null)
		{
			getTrustedFederations().stream().forEach(f -> f.toProperties(raw));
		}

		if (getIndividualTrustedIdps() != null)
		{
			getIndividualTrustedIdps().stream().forEach(f -> f.toProperties(raw, msg, fileService, name));
		}

		raw.put(SAMLSPProperties.P + SAMLSPProperties.PUBLISH_METADATA, String.valueOf(isPublishMetadata()));
		if (getMetadataPath() != null)
		{
			raw.put(SAMLSPProperties.P + SAMLSPProperties.METADATA_PATH, getMetadataPath());
		}

		raw.put(SAMLSPProperties.P + SAMLSPProperties.SIGN_METADATA, String.valueOf(isSignMetadata()));

		if (getMetadataSource() != null && !isAutoGenerateMetadata())
		{
			FileFieldUtils.saveInProperties(getMetadataSource(),
					SAMLSPProperties.P + SAMLSPProperties.METADATA_SOURCE, raw, fileService,
					StandardOwner.SERVICE.toString(), name);
		}

		if (getSloPath() != null)
		{
			raw.put(SAMLSPProperties.P + SAMLSPProperties.SLO_PATH, getSloPath());
		}

		if (getSloRealm() != null)
		{
			raw.put(SAMLSPProperties.P + SAMLSPProperties.SLO_REALM, getSloRealm());
		}

		if (getSloMappings() != null)
		{
			getSloMappings().forEach(m -> {

				int i = getSloMappings().indexOf(m) + 1;
				raw.put(SAMLSPProperties.P + SAMLSPProperties.IDENTITY_MAPPING_PFX + i + "."
						+ SAMLSPProperties.IDENTITY_LOCAL,
						m.getUnityId() == null ? "" : m.getUnityId());

				raw.put(SAMLSPProperties.P + SAMLSPProperties.IDENTITY_MAPPING_PFX + i + "."
						+ SAMLSPProperties.IDENTITY_SAML,
						m.getSamlId() == null ? "" : m.getSamlId());

			});
		}

		SAMLSPProperties samlProp = new SAMLSPProperties(raw, pkiMan);
		return samlProp.getAsString();

	}

	public void fromProperties(PKIManagement pkiMan, URIAccessService uriAccessService, 
			ImageAccessService imageAccessService, MessageSource msg, String properties)
	{
		Properties raw = new Properties();
		try
		{
			raw.load(new StringReader(properties));
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator", e);
		}

		SAMLSPProperties samlSpProp = new SAMLSPProperties(raw, pkiMan);

		setRequesterId(samlSpProp.getValue(SAMLSPProperties.REQUESTER_ID));
		setCredential(samlSpProp.getValue(SAMLSPProperties.CREDENTIAL));
		setAdditionalCredential(samlSpProp.getValue(SAMLSPProperties.ADDITIONAL_CREDENTIAL));
		setIncludeAddtionalCredentialInMetadata(samlSpProp.getBooleanValue(SAMLSPProperties.INCLUDE_ADDITIONAL_CREDENTIAL_IN_METADATA));
		setAcceptedNameFormats(samlSpProp.getListOfValues(SAMLSPProperties.ACCEPTED_NAME_FORMATS));
		setRequireSignedAssertion(samlSpProp.getBooleanValue(SAMLSPProperties.REQUIRE_SIGNED_ASSERTION));
		setDefSignRequest(samlSpProp.getBooleanValue(SAMLSPProperties.DEF_SIGN_REQUEST));
		String defNameFormat = samlSpProp.getValue(SAMLSPProperties.DEF_REQUESTED_NAME_FORMAT);
		setDefaultRequestedNameFormat(defNameFormat != null ? Arrays.asList(defNameFormat) : null);
		if (samlSpProp.isSet(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION))
		{
			setDefAccountAssociation(
					samlSpProp.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION));
		}

		Set<String> fedKeys = samlSpProp.getStructuredListKeys(SAMLSPProperties.IDPMETA_PREFIX);

		trustedFederations = new ArrayList<>();
		fedKeys.forEach(

				key -> {
					SAMLAuthnTrustedFederationConfiguration fed = new SAMLAuthnTrustedFederationConfiguration();
					key = key.substring(SAMLSPProperties.IDPMETA_PREFIX.length(), key.length() - 1);
					fed.fromProperties(samlSpProp, key);
					trustedFederations.add(fed);
				});

		Set<String> idpKeys = samlSpProp.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);

		individualTrustedIdps = new ArrayList<>();
		idpKeys.forEach(

				key -> {
					SAMLIndividualTrustedSamlIdpConfiguration idp = new SAMLIndividualTrustedSamlIdpConfiguration();
					key = key.substring(SAMLSPProperties.IDP_PREFIX.length(), key.length() - 1);
					idp.fromProperties(msg, imageAccessService, samlSpProp, key);
					individualTrustedIdps.add(idp);
				});

		if (samlSpProp.isSet(SamlProperties.PUBLISH_METADATA))
		{
			setPublishMetadata(samlSpProp.getBooleanValue(SamlProperties.PUBLISH_METADATA));
		}

		setMetadataPath(samlSpProp.getValue(SAMLSPProperties.METADATA_PATH));

		if (samlSpProp.isSet(SamlProperties.SIGN_METADATA))
		{
			setSignMetadata(samlSpProp.getBooleanValue(SamlProperties.SIGN_METADATA));
		}

		if (samlSpProp.isSet(SamlProperties.METADATA_SOURCE))
		{
			setAutoGenerateMetadata(false);

			String metaUri = samlSpProp.getValue(SamlProperties.METADATA_SOURCE);

			try
			{
				URI uri = URIHelper.parseURI(metaUri);
				if (URIHelper.isWebReady(uri))
				{
					setMetadataSource(new LocalOrRemoteResource(uri.toString()));
				} else
				{
					FileData fileData = uriAccessService.readURI(uri);
					setMetadataSource(new LocalOrRemoteResource(fileData.getContents(),
							uri.toString()));
				}

			} catch (Exception e)
			{
				log.error("Can not load configured metadata from uri: " + metaUri);
			}
		} else
		{
			setAutoGenerateMetadata(true);
		}

		setSloPath(samlSpProp.getValue(SAMLSPProperties.SLO_PATH));
		setSloRealm(samlSpProp.getValue(SAMLSPProperties.SLO_REALM));

		Set<String> sloMappingsKeys = samlSpProp.getStructuredListKeys(SAMLSPProperties.IDENTITY_MAPPING_PFX);

		sloMappings = new ArrayList<>();
		sloMappingsKeys.forEach(

				key -> {
					SAMLIdentityMapping m = new SAMLIdentityMapping();
					if (samlSpProp.getValue(key + SAMLSPProperties.IDENTITY_LOCAL) != null
							&& !samlSpProp.getValue(key + SAMLSPProperties.IDENTITY_LOCAL)
									.isEmpty())
					{
						m.setUnityId(samlSpProp
								.getValue(key + SAMLSPProperties.IDENTITY_LOCAL));
					}

					if (samlSpProp.getValue(key + SAMLSPProperties.IDENTITY_SAML) != null
							&& !samlSpProp.getValue(key + SAMLSPProperties.IDENTITY_SAML)
									.isEmpty())
					{
						m.setSamlId(samlSpProp.getValue(key + SAMLSPProperties.IDENTITY_SAML));
					}
					sloMappings.add(m);
				});
	}

	public String getRequesterId()
	{
		return requesterId;
	}

	public void setRequesterId(String requesterId)
	{
		this.requesterId = requesterId;
	}

	public String getCredential()
	{
		return credential;
	}

	public void setCredential(String credential)
	{
		this.credential = credential;
	}

	public List<String> getAcceptedNameFormats()
	{
		return acceptedNameFormats;
	}

	public void setAcceptedNameFormats(List<String> acceptedNameFormats)
	{
		this.acceptedNameFormats = acceptedNameFormats;
	}

	public boolean isRequireSignedAssertion()
	{
		return requireSignedAssertion;
	}

	public void setRequireSignedAssertion(boolean requireSignedAssertion)
	{
		this.requireSignedAssertion = requireSignedAssertion;
	}

	public boolean isDefSignRequest()
	{
		return defSignRequest;
	}

	public void setDefSignRequest(boolean defSignRequest)
	{
		this.defSignRequest = defSignRequest;
	}

	public List<String> getDefaultRequestedNameFormat()
	{
		return defaultRequestedNameFormat;
	}

	public void setDefaultRequestedNameFormat(List<String> defaultRequestedNameFormat)
	{
		this.defaultRequestedNameFormat = defaultRequestedNameFormat;
	}

	public boolean isDefAccountAssociation()
	{
		return defAccountAssociation;
	}

	public void setDefAccountAssociation(boolean defAccountAssociation)
	{
		this.defAccountAssociation = defAccountAssociation;
	}

	public List<SAMLAuthnTrustedFederationConfiguration> getTrustedFederations()
	{
		return trustedFederations;
	}

	public void setTrustedFederations(List<SAMLAuthnTrustedFederationConfiguration> trustedFederations)
	{
		this.trustedFederations = trustedFederations;
	}

	public List<SAMLIndividualTrustedSamlIdpConfiguration> getIndividualTrustedIdps()
	{
		return individualTrustedIdps;
	}

	public void setIndividualTrustedIdps(List<SAMLIndividualTrustedSamlIdpConfiguration> individualTrustedIdps)
	{
		this.individualTrustedIdps = individualTrustedIdps;
	}

	public boolean isPublishMetadata()
	{
		return publishMetadata;
	}

	public void setPublishMetadata(boolean publishMetadata)
	{
		this.publishMetadata = publishMetadata;
	}

	public String getMetadataPath()
	{
		return metadataPath;
	}

	public void setMetadataPath(String metadataPath)
	{
		this.metadataPath = metadataPath;
	}

	public boolean isSignMetadata()
	{
		return signMetadata;
	}

	public void setSignMetadata(boolean signMetadata)
	{
		this.signMetadata = signMetadata;
	}

	public String getSloPath()
	{
		return sloPath;
	}

	public void setSloPath(String sloPath)
	{
		this.sloPath = sloPath;
	}

	public String getSloRealm()
	{
		return sloRealm;
	}

	public void setSloRealm(String sloRealm)
	{
		this.sloRealm = sloRealm;
	}

	public List<SAMLIdentityMapping> getSloMappings()
	{
		return sloMappings;
	}

	public void setSloMappings(List<SAMLIdentityMapping> sloMappings)
	{
		this.sloMappings = sloMappings;
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

	public String getAdditionalCredential()
	{
		return additionalCredential;
	}

	public void setAdditionalCredential(String additionallyAdvertisedCredential)
	{
		this.additionalCredential = additionallyAdvertisedCredential;
	}

	public boolean isIncludeAddtionalCredentialInMetadata()
	{
		return includeAddtionalCredentialInMetadata;
	}

	public void setIncludeAddtionalCredentialInMetadata(boolean includeAddtionalCredentialInMetadata)
	{
		this.includeAddtionalCredentialInMetadata = includeAddtionalCredentialInMetadata;
	}
}