/*
 * Copyright (c) 2007-2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.saml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.unicore.samly2.SAMLBindings;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.MetadataSignatureValidation;

/**
 * Properties-based configuration of SAML endpoint.
 * This class is a base for extension by SP and IdP specific classes.
 * @author K. Benedyczak
 */
public abstract class SamlProperties extends UnityPropertiesHelper
{
	public static final String P = "unity.saml.";
	
	/**
	 * Note: it is intended that {@link SAMLBindings} is not used here: we want to have only the 
	 * supported bindings here. However the names here must be exactly the same as in {@link SAMLBindings}.
	 * Note that adding a new binding here requires a couple of changes in the code. 
	 * E.g. support in SAML Metadata-> config conversion, ECP, web retrieval, ....
	 */
	public enum Binding {HTTP_REDIRECT, HTTP_POST, SOAP};
	
	public static final String PUBLISH_METADATA = "publishMetadata";
	public static final String SIGN_METADATA = "signMetadata";
	public static final String METADATA_SOURCE = "metadataSource";
	
	
	public static final String METADATA_URL = "url";
	public static final String METADATA_HTTPS_TRUSTSTORE = "httpsTruststore";
	public static final String METADATA_REFRESH = "refreshInterval";
	public static final String METADATA_SIGNATURE = "signaturVerification";
	public static final String METADATA_ISSUER_CERT = "signatureVerificationCertificate";
	
	//defined here and used in SP and IDP properties
	public static final String REDIRECT_LOGOUT_URL = "redirectLogoutEndpoint";
	public static final String POST_LOGOUT_URL = "postLogoutEndpoint";
	public static final String REDIRECT_LOGOUT_RET_URL = "redirectLogoutResponseEndpoint";
	public static final String POST_LOGOUT_RET_URL = "postLogoutResponseEndpoint";
	public static final String SOAP_LOGOUT_URL = "soapLogoutEndpoint";
	
	public static final String IDENTITY_MAPPING_PFX = "identityMapping.";
	public static final String IDENTITY_LOCAL = "localIdentity";
	public static final String IDENTITY_SAML = "samlIdentity";
	
	public static final DocumentationCategory samlMetaCat = new DocumentationCategory("SAML metadata settings", "6");
	public static final DocumentationCategory remoteMeta = new DocumentationCategory(
			"Configuration read from trusted SAML metadata", "02");

	public static Map<String, PropertyMD> getDefaults(String metasPrefix, String metasDesc)
	{
		Map<String, PropertyMD> defaults = new HashMap<>();
		defaults.put(PUBLISH_METADATA, new PropertyMD("true").setCategory(samlMetaCat).
				setDescription("Controls whether the SAML Metadata should be published."));
		defaults.put(SIGN_METADATA, new PropertyMD("false").setCategory(samlMetaCat).
				setDescription("Controls whether the SAML Metadata should be automatically signed " +
						"before publishing it. " +
						"If a publication of a custom matadata from file is confiured " +
						"which is already signed, do not turn this option on, as then metadata will be signed twice."));
		defaults.put(METADATA_SOURCE, new PropertyMD().setPath().setCategory(samlMetaCat).
				setDescription("If undefined then metadata is automatically generated. If this option is defined, " +
						"then it should contain a file path, to a file with custom metadata document. " +
						"This document will be published as-is, " +
						"however it will be checked first for correctness."));


		defaults.put(metasPrefix, new PropertyMD().setCategory(remoteMeta).setStructuredList(false).
				setDescription(metasDesc));	
		defaults.put(METADATA_URL, new PropertyMD().setCategory(remoteMeta).setMandatory().
				setStructuredListEntry(metasPrefix).setDescription(
				"URL with the metadata location. Can be local or HTTP(s) URL. "
				+ "In case of HTTPS the server's certificate will be checked against the main "
				+ "Unity server's truststore"
				+ " only if " + METADATA_HTTPS_TRUSTSTORE + " is set."));
		defaults.put(METADATA_REFRESH, new PropertyMD("3600").setCategory(remoteMeta).
				setStructuredListEntry(metasPrefix).setDescription(
				"How often the metadata should be reloaded."));
		defaults.put(METADATA_HTTPS_TRUSTSTORE, new PropertyMD().setCategory(remoteMeta).
				setStructuredListEntry(metasPrefix).setDescription(
				"If set then the given truststore will be used for HTTPS connection validation during "
				+ "metadata fetching. Otherwise the default Java trustststore will be used."));
		defaults.put(METADATA_SIGNATURE, new PropertyMD(MetadataSignatureValidation.ignore).
				setCategory(remoteMeta).setStructuredListEntry(metasPrefix).setDescription(
				"Controls whether metadata signatures should be checked. If checking is turned on then "
				+ "the validation certificate must be set."));
		defaults.put(METADATA_ISSUER_CERT, new PropertyMD().setCategory(remoteMeta).
				setStructuredListEntry(metasPrefix).setDescription(
				"Name of certificate to check metadata signature. Used only if signatures "
				+ "checking is turned on."));
		return defaults;
	}

	public SamlProperties(String prefix, Properties properties,
			Map<String, PropertyMD> propertiesMD, Logger log)
	{
		super(prefix, properties, propertiesMD, log);
	}

	protected SamlProperties(SamlProperties cloned)
	{
		super(cloned);
	}

	public synchronized Properties getProperties()
	{
		Properties copy = new Properties();
		copy.putAll(properties);
		return copy;
	}
	
	@Override
	public abstract SamlProperties clone();	
	
	public abstract Properties getSourceProperties();
	
	public List<SAMLEndpointDefinition> getLogoutEndpointsFromStructuredList(String configKey)
	{
		String postSlo = getValue(configKey + POST_LOGOUT_URL);
		String redirectSlo = getValue(configKey + REDIRECT_LOGOUT_URL);
		String postRetSlo = getValue(configKey + POST_LOGOUT_RET_URL);
		String redirectRetSlo = getValue(configKey + REDIRECT_LOGOUT_RET_URL);
		String soapSlo = getValue(configKey + SOAP_LOGOUT_URL);

		if (redirectRetSlo == null)
			redirectRetSlo = redirectSlo;
		if (postRetSlo == null)
			postRetSlo = postSlo;
		
		List<SAMLEndpointDefinition> ret = new ArrayList<>(3);
		if (postSlo != null)
			ret.add(new SAMLEndpointDefinition(Binding.HTTP_POST, postSlo, postRetSlo));
		if (redirectSlo != null)
			ret.add(new SAMLEndpointDefinition(Binding.HTTP_REDIRECT, redirectSlo, redirectRetSlo));
		if (soapSlo != null)
			ret.add(new SAMLEndpointDefinition(Binding.SOAP, soapSlo, soapSlo));
		return ret;
	}
	
	protected Set<String> getCertificateNames(String idpKey, String singleProp, String multiProp)
	{
		Set<String> idpCertNames = new HashSet<>();
		if (isSet(idpKey+singleProp))
			idpCertNames.add(getValue(idpKey+singleProp));
		idpCertNames.addAll(getListOfValues(idpKey+multiProp));
		return idpCertNames;
	}
	

}
