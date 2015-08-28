/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.afterlogin;

import java.net.MalformedURLException;
import java.net.URL;
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
 * Configuration of the standalone account association feature.
 * 
 * @author K. Benedyczak
 */
public class ConnectIdWellKnownURLProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, ConnectIdWellKnownURLProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.endpoint.connectId.";
	
	public static final String REDIRECT_URL = "redirectUrl";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static
	{
		META.put(REDIRECT_URL, new PropertyMD().
				setDescription("If defined must must contain a valid URL "
						+ "to which the requester is redirected after completing the account"
						+ "association process. Additional status information is provided"
						+ "with query parameters."));
	}
	
	public ConnectIdWellKnownURLProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
		
		if (isSet(REDIRECT_URL))
		{
			try
			{
				new URL(getValue(REDIRECT_URL));
			} catch (MalformedURLException e)
			{
				throw new ConfigurationException("Redirect URL is invalid", e);
			}
		}
	}
}
