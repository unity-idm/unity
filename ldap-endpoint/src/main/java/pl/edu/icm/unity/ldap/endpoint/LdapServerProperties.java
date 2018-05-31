/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.base.utils.Log;

/**
 * Configuration of LDAP server.
 */
public class LdapServerProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, LdapServerProperties.class);
	
	@DocumentationReferencePrefix
	public static final String PREFIX = "unity.ldapServer.";
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META = new HashMap<>();
	
	public static final String HOST = "host";
	public static final String LDAP_PORT = "ldapPort";
	public static final String LDAPS_PORT = "ldapsPort";
	public static final String TLS_SUPPORT = "tls";
	public static final String CREDENTIAL = "credential";
	public static final String GROUP_MEMBER = "groupMember";
	public static final String GROUP_MEMBER_DN_REGEXP = "groupMemberDnRegexp";
	public static final String GROUP_OF_NAMES_RETURN_FORMAT = "groupOfNamesReturnFormat";
	public static final String RETURNED_USER_ATTRIBUTES = "returnedUserAttributes";
	public static final String USER_NAME_ALIASES = "userNameAliases";
        public static final String ATTRIBUTES_MAP_PFX = "attributes.";
        public static final String ATTRIBUTES_MAP_UNITY_IDENTITY = "unity.identity";
        public static final String ATTRIBUTES_MAP_UNITY_ATRIBUTE = "unity.attribute";
        public static final String ATTRIBUTES_MAP_LDAP_AT = "ldap.at";
        public static final String ATTRIBUTES_MAP_LDAP_OID = "ldap.oid";
        
	static
	{
		PropertyMD.DocumentationCategory main = new PropertyMD.DocumentationCategory("General settings", "1");
		META.put(HOST, new PropertyMD().setCategory(main)
			.setDescription("LDAP server host settings"));
		META.put(LDAP_PORT, new PropertyMD().setCategory(main)
			.setDescription("LDAP server port settings"));
		META.put(LDAPS_PORT, new PropertyMD().setCategory(main)
			.setDescription("LDAPs server port settings"));

		META.put(TLS_SUPPORT, new PropertyMD().setCategory(main)
			.setDescription("LDAP tls support"));
		META.put(CREDENTIAL, new PropertyMD().setCategory(main).setMandatory()
			.setDescription("Name of Unity credential that will be used as LDAP server's own identity"));

		META.put(GROUP_MEMBER_DN_REGEXP, new PropertyMD().setCategory(main)
			.setDescription("Regular expression that should match the DN of a member compare request." +
				"Optionally, specifying the group that defines the group name."));

		META.put(GROUP_OF_NAMES_RETURN_FORMAT, new PropertyMD().setCategory(main)
			.setDescription("Return DN format of groupOfNames search request."));

		META.put(GROUP_MEMBER, new PropertyMD().setCategory(main)
			.setDescription("String identifying the LDAP member comparison request - " +
				"DN of a group asking for membership of a user in compare attribute " +
				"value."));
		META.put(RETURNED_USER_ATTRIBUTES, new PropertyMD().setCategory(main)
			.setDescription("Attributes that should be returned if return all user attributes flag is set"));
		META.put(USER_NAME_ALIASES, new PropertyMD().setCategory(main).setMandatory()
			.setDescription("Comma separated list of attributes that are used to extract username from DN"));
                
                META.put(ATTRIBUTES_MAP_PFX, new PropertyMD().setStructuredList(true).setCategory(main)
			.setDescription("Unity attribute to LDAP attribute mappings defined under this prefix"));
                META.put(ATTRIBUTES_MAP_UNITY_IDENTITY, new PropertyMD()
                		.setMandatory().setCategory(main).setStructuredListEntry(ATTRIBUTES_MAP_PFX).
				setDescription("Object class of the group."));
                META.put(ATTRIBUTES_MAP_UNITY_ATRIBUTE, new PropertyMD()
                		.setMandatory().setCategory(main).setStructuredListEntry(ATTRIBUTES_MAP_PFX).
				setDescription("Object class of the group."));
                META.put(ATTRIBUTES_MAP_LDAP_AT, new PropertyMD()
                		.setMandatory().setCategory(main).setStructuredListEntry(ATTRIBUTES_MAP_PFX).
				setDescription("LDAP attribute name"));
                META.put(ATTRIBUTES_MAP_LDAP_OID, new PropertyMD()
                		.setMandatory().setCategory(main).setStructuredListEntry(ATTRIBUTES_MAP_PFX).
				setDescription("LDAP attribute OID."));
	}
	
	public LdapServerProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
