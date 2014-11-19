/*
 * Copyright (c) 2007-2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licencing information.
 *
 * Created on Aug 8, 2007
 * Author: K. Benedyczak <golbi@mat.umk.pl>
 */

package pl.edu.icm.unity.saml;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.unicore.samly2.SAMLBindings;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.MetadataSignatureValidation;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;

/**
 * Properties-based configuration of SAML endpoint.
 * This class is a base for extension by SP and IdP specific classes.
 * @author K. Benedyczak
 */
public abstract class SamlProperties extends PropertiesHelper
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
	
	
	public static final DocumentationCategory samlMetaCat = new DocumentationCategory("SAML metadata settings", "6");
	public static final DocumentationCategory remoteMeta = new DocumentationCategory(
			"Configuration read from trusted SAML metadata", "02");

	public static Map<String, PropertyMD> getDefaults(String metasPrefix, String metasDesc)
	{
		Map<String, PropertyMD> defaults = new HashMap<String, PropertyMD>();
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


		defaults.put(METADATA_REFRESH, new PropertyMD("3600").setCategory(remoteMeta).setDescription(
				"How often the metadata should be reloaded."));
		defaults.put(metasPrefix, new PropertyMD().setCategory(remoteMeta).setStructuredList(false).
				setDescription(metasDesc));	
		defaults.put(METADATA_URL, new PropertyMD().setCategory(remoteMeta).setMandatory().
				setStructuredListEntry(metasPrefix).setDescription(
				"URL with the metadata location. Can be local or HTTP(s) URL. "
				+ "In case of HTTPS the server's certificate will be checked against the main "
				+ "Unity server's truststore"
				+ " only if " + METADATA_HTTPS_TRUSTSTORE + " is set."));
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
			throws ConfigurationException
	{
		super(prefix, properties, propertiesMD, log);
	}

	public synchronized Properties getProperties()
	{
		Properties copy = new Properties();
		copy.putAll(properties);
		return copy;
	}
	
	public abstract SamlProperties clone();	
	
	public abstract Properties getSourceProperties();
}
