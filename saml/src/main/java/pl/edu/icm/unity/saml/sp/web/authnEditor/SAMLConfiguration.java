/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.web.authnEditor;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.file.FileData;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;

/**
 * Main SAML configuration
 * 
 * @author P.Piernik
 *
 */
public class SAMLConfiguration
{
	private static Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLConfiguration.class);
	
	private String requesterId;
	private String credential;
	private List<String> acceptedNameFormats;
	private boolean requireSignedAssertion;
	private boolean defSignRequest;

	private List<String> defaultRequestedNameFormat;
	private String registrationForm;
	private boolean defAccountAssociation;

	private List<TrustedFederationConfiguration> trustedFederations;
	private List<IndividualTrustedSamlIdpConfiguration> individualTrustedIdps;

	private boolean publishMetadata;
	private String metadataPath;
	private boolean signMetadata;
	private boolean autoGenerateMetadata;
	private LocalOrRemoteResource metadataSource;

	
	private String sloPath;
	private String sloRealm;
	private List<SloMapping> sloMappings;

	public SAMLConfiguration()
	{
		setPublishMetadata(true);
	}

	public String toProperties(PKIManagement pkiMan, FileStorageService fileService, String name)
			throws ConfigurationException
	{
		Properties raw = new Properties();
		raw.put(SAMLSPProperties.P + SAMLSPProperties.REQUESTER_ID, getRequesterId());

		if (getCredential() != null)
		{
			raw.put(SAMLSPProperties.P + SAMLSPProperties.CREDENTIAL, getCredential());
		}

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

		if (getRegistrationForm() != null)
		{
			raw.put(SAMLSPProperties.P + CommonWebAuthnProperties.REGISTRATION_FORM, getRegistrationForm());
		}

		raw.put(SAMLSPProperties.P + CommonWebAuthnProperties.ENABLE_ASSOCIATION,
				String.valueOf(isDefAccountAssociation()));

		if (getTrustedFederations() != null)
		{
			getTrustedFederations().stream().forEach(f -> f.toProperties(raw));
		}

		if (getIndividualTrustedIdps() != null)
		{
			getIndividualTrustedIdps().stream().forEach(f -> f.toProperties(raw, fileService, name));
		}

		raw.put(SAMLSPProperties.P + SamlProperties.PUBLISH_METADATA, String.valueOf(isPublishMetadata()));
		if (getMetadataPath() != null)
		{
			raw.put(SAMLSPProperties.P + SAMLSPProperties.METADATA_PATH, getMetadataPath());
		}

		raw.put(SAMLSPProperties.P + SAMLSPProperties.SIGN_METADATA, String.valueOf(isSignMetadata()));

		
		if (getMetadataSource() != null && !isAutoGenerateMetadata())
		{
			FileFieldUtils.saveInProperties(getMetadataSource(), SAMLSPProperties.P + SamlProperties.METADATA_SOURCE, raw, fileService,
					StandardOwner.AUTHENTICATOR.toString(), name);
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

	public void fromProperties(PKIManagement pkiMan, URIAccessService uriAccessService, UnityMessageSource msg,
			String properties)
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
		setAcceptedNameFormats(samlSpProp.getListOfValues(SAMLSPProperties.ACCEPTED_NAME_FORMATS));
		setRequireSignedAssertion(samlSpProp.getBooleanValue(SAMLSPProperties.REQUIRE_SIGNED_ASSERTION));
		setDefSignRequest(samlSpProp.getBooleanValue(SAMLSPProperties.DEF_SIGN_REQUEST));
		String defNameFormat = samlSpProp.getValue(SAMLSPProperties.DEF_REQUESTED_NAME_FORMAT);
		setDefaultRequestedNameFormat(defNameFormat != null ? Arrays.asList(defNameFormat) : null);
		setRegistrationForm(samlSpProp.getValue(CommonWebAuthnProperties.REGISTRATION_FORM));
		if (samlSpProp.isSet(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION))
		{
			setDefAccountAssociation(
					samlSpProp.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION));
		}

		Set<String> fedKeys = samlSpProp.getStructuredListKeys(SAMLSPProperties.IDPMETA_PREFIX);

		trustedFederations = new ArrayList<>();
		fedKeys.forEach(

				key -> {
					TrustedFederationConfiguration fed = new TrustedFederationConfiguration();
					key = key.substring(SAMLSPProperties.IDPMETA_PREFIX.length(), key.length() - 1);
					fed.fromProperties(samlSpProp, key);
					trustedFederations.add(fed);
				});

		Set<String> idpKeys = samlSpProp.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);

		individualTrustedIdps = new ArrayList<>();
		idpKeys.forEach(

				key -> {
					IndividualTrustedSamlIdpConfiguration idp = new IndividualTrustedSamlIdpConfiguration();
					key = key.substring(SAMLSPProperties.IDP_PREFIX.length(), key.length() - 1);
					idp.fromProperties(msg, uriAccessService, samlSpProp, key);
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
					setMetadataSource(new LocalOrRemoteResource(fileData.contents, uri.toString()));
				}
				
				
			} catch (Exception e)
			{
				log.error("Can not load configured metadata from uri: " + metaUri);
			}
		}else {
			setAutoGenerateMetadata(true);
		}
		
		setSloPath(samlSpProp.getValue(SAMLSPProperties.SLO_PATH));
		setSloRealm(samlSpProp.getValue(SAMLSPProperties.SLO_REALM));

		Set<String> sloMappingsKeys = samlSpProp.getStructuredListKeys(SAMLSPProperties.IDENTITY_MAPPING_PFX);

		sloMappings = new ArrayList<>();
		sloMappingsKeys.forEach(

				key -> {
					SloMapping m = new SloMapping();
					if (samlSpProp.getValue(key + SAMLSPProperties.IDENTITY_LOCAL) != null
							&& !samlSpProp.getValue(key + SAMLSPProperties.IDENTITY_LOCAL)
									.isEmpty())
					{
						m.setUnityId(samlSpProp.getValue(key + SAMLSPProperties.IDENTITY_LOCAL));
					}

					if (samlSpProp.getValue(key + SAMLSPProperties.IDENTITY_SAML) != null && !samlSpProp
							.getValue(key + SAMLSPProperties.IDENTITY_SAML).isEmpty())
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

	public String getRegistrationForm()
	{
		return registrationForm;
	}

	public void setRegistrationForm(String registrationForm)
	{
		this.registrationForm = registrationForm;
	}

	public boolean isDefAccountAssociation()
	{
		return defAccountAssociation;
	}

	public void setDefAccountAssociation(boolean defAccountAssociation)
	{
		this.defAccountAssociation = defAccountAssociation;
	}

	public List<TrustedFederationConfiguration> getTrustedFederations()
	{
		return trustedFederations;
	}

	public void setTrustedFederations(List<TrustedFederationConfiguration> trustedFederations)
	{
		this.trustedFederations = trustedFederations;
	}

	public List<IndividualTrustedSamlIdpConfiguration> getIndividualTrustedIdps()
	{
		return individualTrustedIdps;
	}

	public void setIndividualTrustedIdps(List<IndividualTrustedSamlIdpConfiguration> individualTrustedIdps)
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

	public List<SloMapping> getSloMappings()
	{
		return sloMappings;
	}

	public void setSloMappings(List<SloMapping> sloMappings)
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

	public static class SloMapping
	{
		private String unityId;
		private String samlId;

		public SloMapping()
		{

		}

		public String getUnityId()
		{
			return unityId;
		}

		public void setUnityId(String unityId)
		{
			this.unityId = unityId;
		}

		public String getSamlId()
		{
			return samlId;
		}

		public void setSamlId(String samlId)
		{
			this.samlId = samlId;
		}
	}
}