/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.config.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.DocumentationCategory;
import pl.edu.icm.unity.engine.api.config.UnityPropertiesHelper;

public abstract class LDAPCommonProperties extends UnityPropertiesHelper
{
	public enum SearchScope 
	{
		one(com.unboundid.ldap.sdk.SearchScope.ONE), 
		sub(com.unboundid.ldap.sdk.SearchScope.SUB), 
		base(com.unboundid.ldap.sdk.SearchScope.BASE), 
		subordinate(com.unboundid.ldap.sdk.SearchScope.SUBORDINATE_SUBTREE);
		
		com.unboundid.ldap.sdk.SearchScope rScope;
		
		SearchScope(com.unboundid.ldap.sdk.SearchScope rScope)
		{
			this.rScope = rScope;
		}
		
		public com.unboundid.ldap.sdk.SearchScope getInternalScope()
		{
			return rScope;
		}
	}
	
	public enum ConnectionMode {plain, SSL, startTLS};
	
	
	public static final String SERVERS = "servers.";
	public static final String PORTS = "ports.";
	public static final String SOCKET_TIMEOUT = "socketTimeout";
	public static final String FOLLOW_REFERRALS = "referralHopLimit";

	public static final String CONNECTION_MODE = "connectionMode";
	public static final String TLS_TRUST_ALL = "trustAllServerCertificates";

	public static final String USER_ID_EXTRACTOR_REGEXP = "usernameExtractorRegexp";

	//without default
	public static final String USER_DN_TEMPLATE = "userDNTemplate";

	public static final String SEARCH_TIME_LIMIT = "searchTimeLimit";
	public static final String RESULT_ENTRIES_LIMIT = "returnedEntriesLimit";
	
	//without default
	public static final String VALID_USERS_FILTER = "validUsersFilter";

	//without default
	public static final String SYSTEM_DN = "systemDN";
	public static final String SYSTEM_PASSWORD = "systemPassword";

	public static final String TRUSTSTORE = "truststore";

	public static final ConnectionMode DEFAULT_CONNECTION_MODE = ConnectionMode.plain;
	public static final int DEFAULT_FOLLOW_REFERRALS = 2;
	public static final int DEFAULT_SOCKET_TIMEOUT = 30000;
	public static final int DEFAULT_SEARCH_TIME_LIMIT = 30;
	public static final int DEFAULT_RESULT_ENTRIES_LIMIT = 1000;

	public static final DocumentationCategory main = new DocumentationCategory("General settings", "1");
	public static final DocumentationCategory advSearch = new DocumentationCategory(
			"Advanced attribute search settings", "5");

	public static Map<String, PropertyMD> getDefaults()
	{
		Map<String, PropertyMD> defaults = new HashMap<>();
		defaults.put(SERVERS, new PropertyMD().setList(true).setCategory(main).setDescription(
				"List of redundant LDAP server " + "hostnames. Use only one if there is no redundancy."));
		defaults.put(PORTS, new PropertyMD().setList(true).setCategory(main).setDescription(
				"List of redundant LDAP server " + "ports. The ports must match their corresponding servers."));
		defaults.put(CONNECTION_MODE,
				new PropertyMD(DEFAULT_CONNECTION_MODE).setCategory(main).setDescription("It can be controlled "
						+ "whether a connection to teh server should be made using a plain socket, over SSL socket"
						+ "or over a socket with START TLS after handshake."));
		defaults.put(TLS_TRUST_ALL,
				new PropertyMD("false").setCategory(main)
						.setDescription("Used only when TLS mode is enabled. "
								+ "If true then the secured TLS protocol will accept any server's certificate. "
								+ "If false - then the truststore must be configured."));
		defaults.put(SOCKET_TIMEOUT, new PropertyMD(String.valueOf(DEFAULT_SOCKET_TIMEOUT)).setNonNegative()
				.setCategory(main).setDescription("Number of milliseconds the "
						+ "network operations (connect and read) are allowed to lasts. Set to 0 to disable the limit."));
		defaults.put(FOLLOW_REFERRALS,
				new PropertyMD(String.valueOf(DEFAULT_FOLLOW_REFERRALS)).setNonNegative().setCategory(main)
						.setDescription(
								"Number of referrals to follow. " + "Set to 0 to disable following referrals."));
		defaults.put(USER_ID_EXTRACTOR_REGEXP,
				new PropertyMD().setCategory(main).setDescription("This setting is mainly "
						+ "useful when searching for users whose name is given as X.500 name (a DN). If defined "
						+ "must contain a regular expression (Perl style) with a single matching group. "
						+ "This regular expression will be applied for the username provided to the system, "
						+ "and the contents of the matching group will be used instead of the full name, "
						+ "in the '\\{USERNAME\\}' variable. For instance this can be used to get 'uid' "
						+ "attribute value from a DN."));

		defaults.put(SEARCH_TIME_LIMIT, new PropertyMD(String.valueOf(DEFAULT_SEARCH_TIME_LIMIT)).setCategory(main)
				.setDescription("Amount of time (in seconds) "
						+ "for which a search query may be executed. Note that depending on configuration there "
						+ "might be up to two queries performed per a single authentication. The LDAP server "
						+ "might have stricter limit."));
		defaults.put(RESULT_ENTRIES_LIMIT,
				new PropertyMD(String.valueOf(DEFAULT_RESULT_ENTRIES_LIMIT)).setCategory(main)
						.setDescription("Maximum amount of entries that is to be loaded."
								+ "If the limit is exceeded the query will fail. The LDAP server "
								+ "might have stricter limit."));

		defaults.put(TRUSTSTORE, new PropertyMD().setCategory(main)
				.setDescription("Truststore name used to configure client's trust settings for the TLS connections."));

		return defaults;
	}

	public LDAPCommonProperties(String prefix, Properties properties, Map<String, PropertyMD> propertiesMD, Logger log)
	{
		super(prefix, properties, propertiesMD, log);
	}
}
