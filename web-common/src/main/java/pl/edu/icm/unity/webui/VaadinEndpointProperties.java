/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

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
 * Generic settings for all Vaadin web-endpoints. May be converted to JSON later on.
 * @author K. Benedyczak
 */
public class VaadinEndpointProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, VaadinEndpointProperties.class);
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.endpoint.web.";
	
	public static final String SESSION_TIMEOUT = "sessionTimeout";
	public static final String PRODUCTION_MODE = "productionMode";
	public static final String BLOCK_AFTER_UNSUCCESSFUL = "blockAfterUnsuccessfulLogins";
	public static final String BLOCK_FOR = "blockFor";
	public static final String ENABLE_REGISTRATION = "enableRegistration";
	public static final String ENABLED_REGISTRATION_FORMS = "enabledRegistrationForms.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	
	static
	{
		META.put(SESSION_TIMEOUT, new PropertyMD("600").setPositive().
				setDescription("Defines maximum validity period (in seconds) of a web session."));
		META.put(PRODUCTION_MODE, new PropertyMD("true").setHidden().
				setDescription("Controls wether Vaadin should work in production mode or in debug mode (false)."));
		META.put(BLOCK_AFTER_UNSUCCESSFUL, new PropertyMD("5").setPositive().
				setDescription("Defines maximum number of unsuccessful logins before the access is temporarely blocked."));
		META.put(BLOCK_FOR, new PropertyMD("60").setPositive().
				setDescription("Defines for how long (in seconds) the access should be blocked for the" +
						"client reaching the limit of unsuccessful logins."));
		META.put(ENABLE_REGISTRATION, new PropertyMD("false").
				setDescription("Controls if registration option should be allowed for an endpoint."));
		META.put(ENABLED_REGISTRATION_FORMS, new PropertyMD().setList(false).
				setDescription("Defines which registration forms should be enabled for the endpoint. " +
						"Values are form names. If the form with given name doesn't exist it will be ignored." +
						"If there are no forms defined with this property, then all public forms are made available."));
		
	}
	
	public VaadinEndpointProperties(Properties properties)
			throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}

}
