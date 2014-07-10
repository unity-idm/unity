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
	
	public static final String PUBLISH_METADATA = "publishMetadata";
	public static final String SIGN_METADATA = "signMetadata";
	public static final String METADATA_SOURCE = "metadataSource";
	
	public static final DocumentationCategory samlMetaCat = new DocumentationCategory("SAML metadata settings", "6");
	public final static Map<String, PropertyMD> defaults=new HashMap<String, PropertyMD>();
	
	static
	{
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
}
