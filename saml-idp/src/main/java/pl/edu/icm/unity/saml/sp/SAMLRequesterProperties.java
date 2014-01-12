/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.saml.NameFormat;
import pl.edu.icm.unity.server.utils.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of a SAML requester (or SAML SP).
 * @author K. Benedyczak
 */
public class SAMLRequesterProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, SAMLRequesterProperties.class);
	
	@DocumentationReferencePrefix
	public static final String P = "unity.saml.requester.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	public static final String REQUESTER_ID = "requesterEntityId";
	public static final String SIGN_REQUEST = "signRequest";
	public static final String REQUESTED_NAME_FORMAT = "requestedNameFormat";
	public static final String ACCEPTED_NAME_FORMATS = "acceptedNameFormats";
	
	
	public static final String TRANSLATION_PROFILE = "translationProfile";
	
	static
	{
		META.put(REQUESTER_ID, new PropertyMD().setMandatory().setDescription(
				"SAML entity ID (must be a URI) of the lcoal SAML requester (or service provider)."));
		META.put(SIGN_REQUEST, new PropertyMD("true").setDescription(
				"Controls whether the requests should be signed."));
		META.put(REQUESTED_NAME_FORMAT, new PropertyMD().setEnum(NameFormat.emailAddress).setDescription(
				"If defined then specifies what SAML name format should be requested from the IdP." +
				" If undefined then IdP is free to choose, however see the " + ACCEPTED_NAME_FORMATS +
				" property."));
		META.put(ACCEPTED_NAME_FORMATS, new PropertyMD().setList(false).setDescription(
				"If defined then specifies what SAML name formatd are accepted from IdP. " +
				"Useful when the property " + REQUESTED_NAME_FORMAT + " is undefined. " +
				"If this property is defined then this setting is ignored. Allowed values are the same" +
				"as for the " + REQUESTED_NAME_FORMAT + "."));
		
		META.put(TRANSLATION_PROFILE, new PropertyMD().setMandatory().setDescription("Name of a translation" +
				" profile, which will be used to map remotely obtained attributes and identity" +
				" to the local counterparts. The profile should at least map the remote identity."));
	}

	
	public SAMLRequesterProperties(Properties properties) throws ConfigurationException
	{
		super(P, properties, META, log);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
