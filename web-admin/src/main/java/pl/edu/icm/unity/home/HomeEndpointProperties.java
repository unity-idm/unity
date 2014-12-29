/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.home;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Configuration of the Home endpoint.
 * 
 * @author K. Benedyczak
 */
public class HomeEndpointProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, HomeEndpointProperties.class);

	public enum Components {credential, preferences, userDetails};
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.userhome.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();

	public static final String DISABLED_COMPONENTS = "disabledComponents.";
	
	static
	{
		META.put(DISABLED_COMPONENTS, new PropertyMD().setList(false).
				setDescription("List of tags of UI components "
				+ "which should be disabled. Valid tags: '" + 
						Arrays.toString(Components.values()) + "'"));
	}
	
	public HomeEndpointProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
	
	public Set<String> getDisabledComponents()
	{
		return new HashSet<>(getListOfValues(DISABLED_COMPONENTS));
	}
}
