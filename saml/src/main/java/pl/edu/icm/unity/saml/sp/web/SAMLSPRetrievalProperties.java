/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import eu.unicore.samly2.SAMLBindings;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of a SAML web retrieval.
 * 
 * @author K. Benedyczak
 */
public class SAMLSPRetrievalProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, SAMLSPRetrievalProperties.class);
	
	/**
	 * Note: it is intended that {@link SAMLBindings} is not used here: we want to have only the 
	 * supported bindings here.
	 */
	public enum Binding {httpRedirect, httpPost};
	
	@DocumentationReferencePrefix
	public static final String P = "unity.saml.webretrieval.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	public static final String DISPLAY_NAME = "displayName";
	public static final String IDP_PREFIX = "remoteIdp.";
	public static final String IDP_NAME = "name";
	public static final String IDP_ADDRESS = "address";
	public static final String IDP_BINDING = "binding";
	
	static
	{
		META.put(IDP_PREFIX, new PropertyMD().setStructuredList(true).setMandatory().setDescription(
				"With this prefix configuration of trusted and enabled remote SAML IdPs is stored. " +
				"There must be at least one IdP defined. If there are multiple ones defined, then the user can choose which one to use."));
		META.put(IDP_ADDRESS, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setMandatory().setDescription(
				"Address of the IdP endpoint."));
		META.put(IDP_BINDING, new PropertyMD(Binding.httpPost).setStructuredListEntry(IDP_PREFIX).setDescription(
				"SAML binding to be used to send a request to the IdP."));
		META.put(IDP_NAME, new PropertyMD().setStructuredListEntry(IDP_PREFIX).setDescription(
				"Displayed name of the IdP. If not defined then the name is created " +
				"from the IdP address (what is rather not user friendly)."));
		
		META.put(DISPLAY_NAME, new PropertyMD("SAML authentication").setDescription(
				"Name of the SAML authentication GUI component"));
	}

	
	public SAMLSPRetrievalProperties(Properties properties) throws ConfigurationException
	{
		super(P, properties, META, log);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
