/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client.config;

import eu.unicore.util.configuration.DocumentationReferenceMeta;
import eu.unicore.util.configuration.DocumentationReferencePrefix;
import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import io.imunity.vaadin.auth.CommonWebAuthnProperties;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.ldap.client.config.common.LDAPConnectionProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Low level configuration handling - implemented with {@link Properties} as storage format.
 * @author K. Benedyczak
 */
public class LdapProperties extends LDAPConnectionProperties
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG, LdapProperties.class);
	
	public enum BindAs {user, system, none}

	@DocumentationReferencePrefix
	public static final String PREFIX = "ldap.";
	
	public static final String BIND_AS = "bindAs";
	
	public static final String BIND_ONLY = "authenticateOnly";
	public static final String ATTRIBUTES = "attributes.";
	public static final String MEMBER_OF_ATTRIBUTE = "memberOfAttribute";
	public static final String MEMBER_OF_GROUP_ATTRIBUTE = "memberOfGroupAttribute";

	public static final String USER_DN_SEARCH_KEY = "userDNSearchKey";
	
	public static final String ADV_SEARCH_PFX = "additionalSearch.";
	public static final String ADV_SEARCH_ATTRIBUTES = "selectedAttributes";
	public static final String ADV_SEARCH_FILTER = "filter";
	public static final String ADV_SEARCH_BASE = "baseName";
	public static final String ADV_SEARCH_SCOPE = "scope";
	
	public static final String GROUPS_BASE_NAME = "groupsBaseName";
	public static final String GROUPS_SEARCH_IN_LDAP = "delegateGroupFiltering";
	public static final String GROUP_DEFINITION_PFX = "groups.";
	public static final String GROUP_DEFINITION_OC = "objectClass";
	public static final String GROUP_DEFINITION_MEMBER_ATTR = "memberAttribute";
	public static final String GROUP_DEFINITION_NAME_ATTR = "nameAttribute";
	public static final String GROUP_DEFINITION_MATCHBY_MEMBER_ATTR = "matchByMemberAttribute";
	
	public static final String DEFAULT_TRANSLATION_PROFILE = "sys:ldap";
	
	public static final BindAs DEFAULT_BIND_AS = BindAs.user;
	public static final boolean DEFAULT_BIND_ONLY = false;
	public static final boolean DEFAULT_GROUPS_SEARCH_IN_LDAP = true;
	
	@DocumentationReferenceMeta
	public final static Map<String, PropertyMD> META=new HashMap<String, PropertyMD>();
	
	static
	{
		DocumentationCategory groups = new DocumentationCategory("Group retrieval settings", "3");
		DocumentationCategory advSearch = new DocumentationCategory("Advanced attribute search settings", "5");
		META.putAll(getDefaults());

		META.put(BIND_AS, new PropertyMD(DEFAULT_BIND_AS).setCategory(main).setDescription("Fundamental setting "
				+ "controlling how Unity interacts with the LDAP server. By default Unity binds to the LDAP server "
				+ "_as the user_ who is being authenticated by Unity. This may be changed to use a "
				+ "predefined user ('system or unity user') and password. Then the credentials provided"
				+ " by the user are only compared if are genuine, but all searches "
				+ "(and LDAP authorization) is run as the designated system user. In this mode, "
				+ "the system user's DN, password and user's password attribute must be configured. "
				+ "The +user+ option will only work with authenticator and not with importer. "
				+ "Conversly the +none+ option is suitable for use only in case of LDAP import and not in authenticator."));	
		META.put(USER_DN_TEMPLATE, new PropertyMD().setCategory(main).setDescription("Template of a DN of " +
				"the user that should be used to log in. The tempalte must possess a single occurence " +
				"of a special string: '\\{USERNAME\\}'. The username provided by the client" +
				" will be substituted. Mutually exclusive with " + USER_DN_SEARCH_KEY + 
				" and at least one of them must be defined."));
		META.put(USER_DN_SEARCH_KEY, new PropertyMD().setCategory(main).setDescription("A key of one of "
				+ "the advanced search definitions. The search must be defined and must return "
				+ "a single entry. The DN of this entry will be treated as a DN of the user being "
				+ "authenticated. This is useful when the username is not present in the user's DN or "
				+ "when users can have different DN templates. Using this mode is slower then " + USER_DN_TEMPLATE + 
				". Mutually exclusive with " + USER_DN_TEMPLATE + " and at least one of them must be defined."
				+ " To use this mode the " + SYSTEM_DN + " and " + SYSTEM_PASSWORD + " must be also set "
				+ "to run the initial search."));
		META.put(USER_ID_EXTRACTOR_REGEXP, new PropertyMD().setCategory(main).setDescription("This setting is mainly "
				+ "useful when searching for users whose name is given as X.500 name (a DN). If defined "
				+ "must contain a regular expression (Perl style) with a single matching group. "
				+ "This regular expression will be applied for the username provided to the system, "
				+ "and the contents of the matching group will be used instead of the full name, "
				+ "in the '\\{USERNAME\\}' variable. For instance this can be used to get 'uid' "
				+ "attribute value from a DN."));	
		META.put(BIND_ONLY, new PropertyMD(String.valueOf(DEFAULT_BIND_ONLY)).setCategory(main).setDescription("If true then the user is only authenticated" +
				" and no LDAP attributes (including groups) are collected for the user. " +
				"This is much faster but maximally limits an information imported to Unity."));
		META.put(ATTRIBUTES, new PropertyMD().setList(false).setCategory(main).setDescription("List of " +
				"attributes to be retrieved. If the list is empty then all available attributes are fetched."));
		META.put(SYSTEM_DN, new PropertyMD().setCategory(main).setDescription("Relevant and mandatory only if " +
				BIND_AS + " is set to " + BindAs.system + " or when using custom user search. "
				+ "The value must be the DN of the system user to authenticate as before performing any queries."));
		META.put(SYSTEM_PASSWORD, new PropertyMD().setCategory(main).setDescription("Relevant and mandatory only if " +
				BIND_AS + " is set to " + BindAs.system + ". The value must be the password of the system "
				+ "user to authenticate as before performing any queries."));
		
		META.put(GROUPS_BASE_NAME, new PropertyMD().setCategory(groups).setDescription("Base DN under which all groups are defined. " +
				"Groups need not to be immediatelly under this DN. If not defined, then groups " +
				"are not searched for the membership of the user."));
		META.put(GROUPS_SEARCH_IN_LDAP, new PropertyMD(String.valueOf(DEFAULT_GROUPS_SEARCH_IN_LDAP)).setCategory(groups).
				setDescription("If enabled then user's groups are searched at LDAP "
						+ "server using advanced filter. This is much faster however can fail "
						+ "when group member is specified as a DN and not by some siple attribute."));
		META.put(GROUP_DEFINITION_PFX, new PropertyMD().setStructuredList(true).setCategory(groups).setDescription("Group " +
				"definitions should be defined under this prefix."));
		META.put(GROUP_DEFINITION_OC, new PropertyMD().setMandatory().setCategory(groups).setStructuredListEntry(GROUP_DEFINITION_PFX).
				setDescription("Object class of the group."));
		META.put(GROUP_DEFINITION_MEMBER_ATTR, new PropertyMD().setCategory(groups).setMandatory().setStructuredListEntry(GROUP_DEFINITION_PFX).
				setDescription("Group's entry attribute with group members. Usually something like 'member'."));
		META.put(GROUP_DEFINITION_NAME_ATTR, new PropertyMD().setCategory(groups).setStructuredListEntry(GROUP_DEFINITION_PFX).
				setDescription("Group's entry attribute with group's name. If undefined then the whole DN is used."));
		META.put(GROUP_DEFINITION_MATCHBY_MEMBER_ATTR, new PropertyMD().setCategory(groups).setStructuredListEntry(GROUP_DEFINITION_PFX).
				setDescription("If this attribute is defined then it is assumed thet the members in " +
						"the group entry are given with values of a single attribute (e.g. uid), " +
						"not with their full DNs. This property defines this attribute (should " +
						"be present on the user's entry for which groups are searched)."));
		META.put(VALID_USERS_FILTER, new PropertyMD("objectclass=*").setCategory(main).setDescription("Standard LDAP filter of valid users." +
				" Even the users who can authenticate but are not matching this filter will " +
				"have access denied. IMPORTANT: if the '" + BIND_ONLY + "' mode is turned on, this" +
				" setting is ignored."));		
		META.put(MEMBER_OF_ATTRIBUTE, new PropertyMD().setCategory(groups).setDescription("User's attribute name which contains " +
				"groups of the user, usually something like 'memberOf'. If not defined then groups " +
				"are not extracted from the user's entry (but might be retrieved by" +
				" scanning all groups in the LDAP tree)."));
		META.put(MEMBER_OF_GROUP_ATTRIBUTE, new PropertyMD().setCategory(groups).setDescription("If user's attributes are read from " +
				"'memberOf' (or alike) attribute, then this property may be used to extract the actual " +
				"group name from its DN. If undefined then the DN will be used as group's name. " +
				"If defined then the group's name will be the value of the attribute in the group's DN " +
				"with a name defined here."));

		META.put(ADV_SEARCH_PFX, new PropertyMD().setStructuredList(false).setCategory(advSearch).
				setDescription("Advanced attribute searches can be defined with this prefix."));
		META.put(ADV_SEARCH_BASE, new PropertyMD().setStructuredListEntry(ADV_SEARCH_PFX).setMandatory().setCategory(advSearch).
				setDescription("Base DN for the search.  The value can include a special"
				+ "string: '\\{USERNAME\\}'. The username provided by the client" +
				" will be substituted."));
		META.put(ADV_SEARCH_FILTER, new PropertyMD().setStructuredListEntry(ADV_SEARCH_PFX).setMandatory().setCategory(advSearch).
				setDescription("Filter in LDAP syntax, to match requested entries. The filter can include a special"
				+ "string: '\\{USERNAME\\}'. The username provided by the client" +
				" will be substituted."));
		META.put(ADV_SEARCH_ATTRIBUTES, new PropertyMD().setStructuredListEntry(ADV_SEARCH_PFX).setCategory(advSearch).
				setDescription("Space separated list of attributes to be searched. "
						+ "Attributes from the query will have all values unified from all returned entries by the query."
						+ "Duplicate values will be removed and finally attributes will be added "
						+ "to the set of the standard attributes of the principal."));
		META.put(ADV_SEARCH_SCOPE, new PropertyMD(SearchScope.sub).setStructuredListEntry(ADV_SEARCH_PFX).setCategory(advSearch).
				setDescription("LDAP search scope to be used for this search."));

		META.put(CommonWebAuthnProperties.TRANSLATION_PROFILE, new PropertyMD(DEFAULT_TRANSLATION_PROFILE)
				.setCategory(main)
				.setDescription("Name of a translation"
						+ " profile, which will be used to map remotely obtained attributes and identity"
						+ " to the local counterparts. The profile should at least map the remote identity."));
		META.put(CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE,
				new PropertyMD().setCategory(main).setHidden().setDescription("Translation"
						+ " profile in json, which will be used to map remotely obtained attributes and identity"
						+ " to the local counterparts. The profile should at least map the remote identity."));
	}
	
	public LdapProperties(Properties properties)
	{
		super(PREFIX, properties, META, log);
	}
	
	public LdapProperties(String prefix, Properties properties)
	{
		super(prefix, properties, META, log);
	}
	
	public Properties getProperties()
	{
		return properties;
	}
}
