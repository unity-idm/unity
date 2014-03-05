/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws;

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
 * Generic settings for all CXF WS endpoints. Placeholder - currently no configuration.
 * @author K. Benedyczak
 */
public class CXFEndpointProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, CXFEndpointProperties.class);
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.endpoint.ws.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	
	static
	{
	}
	
	public CXFEndpointProperties(Properties properties)
			throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
