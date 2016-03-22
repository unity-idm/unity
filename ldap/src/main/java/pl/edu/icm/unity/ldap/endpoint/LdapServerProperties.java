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
 */
public class LdapServerProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, LdapServerProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.ldapServer.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();
	
	static
	{
		PropertyMD.DocumentationCategory main = new PropertyMD.DocumentationCategory("General settings", "1");
		META.put("host", new PropertyMD().setCategory(main).setDescription("LDAP server host settings"));
		META.put("ldapPort", new PropertyMD().setCategory(main).setDescription("LDAP server port settings"));
		META.put("ldapsPort", new PropertyMD().setCategory(main).setDescription("LDAPs server port settings"));
		META.put("bindPassword", new PropertyMD().setCategory(main).setDescription("LDAP server bind password"));

		META.put("groupQuery", new PropertyMD().setCategory(main).setDescription("LDAP group query token"));
		META.put("userQuery", new PropertyMD().setCategory(main).setDescription("LDAP user query token"));
		META.put("groupMember", new PropertyMD().setCategory(main).setDescription("LDAP member attribute name"));
		META.put("groupMemberUserRegexp", new PropertyMD().setCategory(main).setDescription("LDAP regexp for getting user from a member query"));
		META.put("returnedUserAttributes", new PropertyMD().setCategory(main).setDescription("Attributes that should be returned if return all user attributes flag is set"));
	}
	
	public LdapServerProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
