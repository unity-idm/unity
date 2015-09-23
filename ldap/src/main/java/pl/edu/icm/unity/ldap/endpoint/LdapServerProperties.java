/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

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
 * Configuration of LDAP server.
 * 
 * @author K. Benedyczak
 */
public class LdapServerProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, LdapServerProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "ldapServer.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();
	
	static
	{
		//TODO - fill META with definitions of requires config options
	}
	
	public LdapServerProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
