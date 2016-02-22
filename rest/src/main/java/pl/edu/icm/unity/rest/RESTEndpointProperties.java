/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Generic settings for all CXF JAX-RS endpoints.
 * 
 * @author K. Benedyczak
 */
public class RESTEndpointProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, RESTEndpointProperties.class);
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.endpoint.rest.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	public static final String ENABLED_CORS_ORIGINS = "allowedCorsOrigins.";
	
	static
	{
		META.put(ENABLED_CORS_ORIGINS, new PropertyMD().setList(false).setDescription(
				"List of origins allowed for the CORS requests. "
				+ "The complete set of HTTP methods is enabled for the enumerated resources. "
				+ "If the list is undefined then CORS support is turned off."));
	}
	
	public RESTEndpointProperties(Properties properties)
			throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
