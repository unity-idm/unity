/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.PropertiesHelper;
import eu.unicore.util.configuration.PropertyMD;

/**
 * Low level configuration handling - implemented with {@link Properties} as storage format.
 * @author K. Benedyczak
 */
public class LdapProperties extends PropertiesHelper
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, LdapProperties.class);
	
	public static final String PREFIX = "ldap.";
	
	public static final String SERVERS = "servers.";
	public static final String PORTS = "ports.";
	public static final String SOCKET_TIMEOUT = "socketTimeout";
	public static final String FOLLOW_REFERRALS = "referralHopLimit";
	
	public static final String USER_DN_TEMPLATE = "userDNTemplate";
	public static final String BIND_ONLY = "authenticateOnly";
	public static final String ATTRIBUTES = "attributes.";
	public static final String SEARCH_TIME_LIMIT = "searchTimeLimit";
	public static final String VALID_USERS_FILTER = "validUsersFilter";
	public static final String MEMBER_OF_ATTRIBUTE = "memberOfAttribute";
	public static final String MEMBER_OF_GROUP_ATTRIBUTE = "memberOfGroupAttribute";
	
	public static final String GROUPS_BASE_NAME = "groupsBaseName";
	public static final String GROUP_DEFINITION_PFX = "groups.";
	public static final String GROUP_DEFINITION_OC = "objectClass";
	public static final String GROUP_DEFINITION_MEMBER_ATTR = "memberAttribute";
	public static final String GROUP_DEFINITION_NAME_ATTR = "nameAttribute";
	public static final String GROUP_DEFINITION_MATCHBY_MEMBER_ATTR = "matchByMemberAttribute";

	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META=new HashMap<String, PropertyMD>();
	
	static
	{
		META.put(SERVERS, new PropertyMD().setList(true).setDescription("List of redundant LDAP server " +
				"hostnames. Use only one if there is no redundancy."));
		META.put(PORTS, new PropertyMD().setList(true).setDescription("List of redundant LDAP server " +
				"ports. The ports must match their corresponding servers."));
		META.put(SOCKET_TIMEOUT, new PropertyMD("30000").setNonNegative().setDescription("Number of milliseconds the " +
				"network operations (connect and read) are allowed to lasts. Set to 0 to disable the limit."));
		META.put(FOLLOW_REFERRALS, new PropertyMD("2").setNonNegative().setDescription("Number of referrals to follow. " +
				"Set to 0 to disable following referrals."));
		META.put(USER_DN_TEMPLATE, new PropertyMD().setMandatory().setDescription("Template of a DN of " +
				"the user that should be used to log in. The tempalte must possess a single occurence " +
				"of a special string: '{USERNAME}' (without quotation). The username provided by the client" +
				" will be substituted."));
		META.put(BIND_ONLY, new PropertyMD("false").setDescription("If true then the user is only authenticated" +
				" and no LDAP attributes (including groups) are collected for the user. " +
				"This is much faster but maximally limits an information imported to Unity."));
		META.put(ATTRIBUTES, new PropertyMD().setList(false).setDescription("List of " +
				"attributes to be retrieved. If the list is empty then all available attributes are fetched."));
		META.put(SEARCH_TIME_LIMIT, new PropertyMD("60").setDescription("Amount of time (in seconds) " +
				"for which a search query may be executed. Note that depending on configuration there " +
				"might be up to two queries performed per a single authentication. The LDAP server " +
				"might have more strict limit."));
		META.put(GROUPS_BASE_NAME, new PropertyMD().setDescription("Base DN under which all groups are defined. " +
				"Groups need not to be immediatelly under this DN. If not defined, then groups " +
				"are not searched for the membership of the user."));
		META.put(GROUP_DEFINITION_PFX, new PropertyMD().setStructuredList(true).setDescription("Group " +
				"definitions should be defined under this prefix."));
		META.put(GROUP_DEFINITION_OC, new PropertyMD().setMandatory().setStructuredListEntry(GROUP_DEFINITION_PFX).
				setDescription("Object class of the group."));
		META.put(GROUP_DEFINITION_MEMBER_ATTR, new PropertyMD().setMandatory().setStructuredListEntry(GROUP_DEFINITION_PFX).
				setDescription("Group's entry attribute with group members. Usually something like 'member'."));
		META.put(GROUP_DEFINITION_NAME_ATTR, new PropertyMD().setStructuredListEntry(GROUP_DEFINITION_PFX).
				setDescription("Group's entry attribute with group's name. If undefined then the whole DN is used."));
		META.put(GROUP_DEFINITION_MATCHBY_MEMBER_ATTR, new PropertyMD().setStructuredListEntry(GROUP_DEFINITION_PFX).
				setDescription("If this attribute is defined then it is assumed thet the members in " +
						"the group entry are given with values of a single attribute (e.g. uid), " +
						"not with their full DNs. This property defines this attribute (should " +
						"be present on the user's entry for which groups are searched)."));

		META.put(VALID_USERS_FILTER, new PropertyMD().setDescription("Standard LDAP filter of valid users." +
				" Even the users who can authenticate but are not matching this filter will " +
				"have access denied. IMPORTANT: if the '" + BIND_ONLY + "' mode is turned on, this" +
				" setting is ignored."));
		
		
		META.put(MEMBER_OF_ATTRIBUTE, new PropertyMD().setDescription("User's attribute name which contains " +
				"groups of the user, usually something like 'memberOf'. If not defined then groups " +
				"are not extracted from the user's entry (but might be retrieved by" +
				" scanning all groups in the LDAP tree)."));
		META.put(MEMBER_OF_GROUP_ATTRIBUTE, new PropertyMD().setDescription("If user's attributes are read from " +
				"'memberOf' (or alike) attribute, then this property may be used to extract the actual " +
				"group name from its DN. If undefined then the DN will be used as group's name. " +
				"If defined then the group's name will be the value of the attribute in the group's DN " +
				"with a name defined here."));
		
	}
	
	public LdapProperties(Properties properties) throws ConfigurationException
	{
		super(PREFIX, properties, META, log);
	}
}
