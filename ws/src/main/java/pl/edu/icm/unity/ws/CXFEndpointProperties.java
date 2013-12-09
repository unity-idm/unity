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
 * Generic settings for all CXF WS endpoints. May be converted to JSON later on.
 * @author K. Benedyczak
 */
public class CXFEndpointProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, CXFEndpointProperties.class);
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.endpoint.ws.";
	
	public static final String BLOCK_AFTER_UNSUCCESSFUL = "blockAfterUnsuccessfulLogins";
	public static final String BLOCK_FOR = "blockFor";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static
	{
		META.put(BLOCK_AFTER_UNSUCCESSFUL, new PropertyMD("5").setPositive().
				setDescription("Defines maximum number of unsuccessful logins before the access is temporarely blocked."));
		META.put(BLOCK_FOR, new PropertyMD("60").setPositive().
				setDescription("Defines for how long (in seconds) the access should be blocked for the" +
						"client reaching the limit of unsuccessful logins."));
	}
	
	public CXFEndpointProperties(Properties properties)
			throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
