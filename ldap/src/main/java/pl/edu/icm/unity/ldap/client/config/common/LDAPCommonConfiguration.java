/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.ldap.client.config.common;

import static pl.edu.icm.unity.ldap.client.LdapUtils.nonEmpty;
import static pl.edu.icm.unity.ldap.client.config.common.LDAPCommonProperties.PORTS;
import static pl.edu.icm.unity.ldap.client.config.common.LDAPCommonProperties.SERVERS;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.ldap.client.config.ServerSpecification;
import pl.edu.icm.unity.ldap.client.config.common.LDAPCommonProperties.ConnectionMode;
import pl.edu.icm.unity.ldap.client.config.common.LDAPCommonProperties.SearchScope;

public abstract class LDAPCommonConfiguration
{
	public enum UserDNResolving
	{
		template, ldapSearch
	}
	
	public static final String USERNAME_TOKEN = "{USERNAME}";
	public static final String USER_DN_SEARCH_KEY = "searchUserDN";

	private String systemDN;
	private String systemPassword;
	private String validUserFilter;
	private UserDNResolving userDNResolving;
	private String userDNTemplate;

	private List<ServerSpecification> servers;
	private ConnectionMode connectionMode;
	private int followReferrals;
	private int searchTimeLimit;
	private int socketTimeout;
	private boolean trustAllCerts;
	private String clientTrustStore;
	private int resultEntriesLimit;

	private String usernameExtractorRegexp;

	// Ldap search option fields
	private String ldapSearchBaseName;
	private String ldapSearchFilter;
	private SearchScope ldapSearchScope;

	
	public LDAPCommonConfiguration()
	{
		setUserDNResolving(UserDNResolving.template);
		setValidUserFilter("objectclass=*");
		servers = new ArrayList<>();
		setConnectionMode(LDAPCommonProperties.DEFAULT_CONNECTION_MODE);
		setFollowReferrals(LDAPCommonProperties.DEFAULT_FOLLOW_REFERRALS);
		setSearchTimeLimit(LDAPCommonProperties.DEFAULT_SEARCH_TIME_LIMIT);
		setSocketTimeout(LDAPCommonProperties.DEFAULT_SOCKET_TIMEOUT);
		setLdapSearchScope(SearchScope.base);
		setResultEntriesLimit(LDAPCommonProperties.DEFAULT_RESULT_ENTRIES_LIMIT);
	}

	public void fromProperties(LDAPCommonProperties ldapProp)
	{
		if (ldapProp.isSet(LDAPCommonProperties.VALID_USERS_FILTER))
		{
			setValidUserFilter(ldapProp.getValue(LDAPCommonProperties.VALID_USERS_FILTER));
		}

		setSystemDN(ldapProp.getValue(LDAPCommonProperties.SYSTEM_DN));
		setSystemPassword(ldapProp.getValue(LDAPCommonProperties.SYSTEM_PASSWORD));
		
		setUserDNTemplate(ldapProp.getValue(LDAPCommonProperties.USER_DN_TEMPLATE));
		List<String> server = ldapProp.getListOfValues(SERVERS);
		List<String> ports = ldapProp.getListOfValues(PORTS);

		for (int i = 0; i < Math.max(server.size(), ports.size()); i++)
		{
			int port = -1;
			try
			{
				port = ports.size() > i ? Integer.parseInt(ports.get(i)) : -1;
			} catch (NumberFormatException e)
			{
				//ok
			}
			servers.add(new ServerSpecification(server.size() > i ? server.get(i) : "", port));

		}

		if (ldapProp.isSet(LDAPCommonProperties.CONNECTION_MODE))
		{
			setConnectionMode(ldapProp.getEnumValue(LDAPCommonProperties.CONNECTION_MODE, ConnectionMode.class));
		}
		if (ldapProp.isSet(LDAPCommonProperties.FOLLOW_REFERRALS))
		{
			setFollowReferrals(ldapProp.getIntValue(LDAPCommonProperties.FOLLOW_REFERRALS));
		}

		if (ldapProp.isSet(LDAPCommonProperties.SEARCH_TIME_LIMIT))
		{
			setSearchTimeLimit(ldapProp.getIntValue(LDAPCommonProperties.SEARCH_TIME_LIMIT));
		}
		if (ldapProp.isSet(LDAPCommonProperties.SOCKET_TIMEOUT))
		{
			setSocketTimeout(ldapProp.getIntValue(LDAPCommonProperties.SOCKET_TIMEOUT));
		}
		if (ldapProp.isSet(LDAPCommonProperties.RESULT_ENTRIES_LIMIT))
		{
			setResultEntriesLimit(ldapProp.getIntValue(LDAPCommonProperties.RESULT_ENTRIES_LIMIT));
		}

		setTrustAllCerts(ldapProp.getBooleanValue(LDAPCommonProperties.TLS_TRUST_ALL));
		setClientTrustStore(ldapProp.getValue(LDAPCommonProperties.TRUSTSTORE));
		setUsernameExtractorRegexp(ldapProp.getValue(LDAPCommonProperties.USER_ID_EXTRACTOR_REGEXP));
	}

	public void toProperties(String prefix, Properties raw , MessageSource msg) throws ConfigurationException
	{
		if (getValidUserFilter() != null)
		{
			raw.put(prefix + LDAPCommonProperties.VALID_USERS_FILTER, getValidUserFilter());
		}

		for (int i = 0; i < servers.size(); i++)
		{
			ServerSpecification servConfig = servers.get(i);
			raw.put(prefix + LDAPCommonProperties.SERVERS + (i + 1), servConfig.getServer());
			raw.put(prefix + LDAPCommonProperties.PORTS + (i + 1),
					String.valueOf(servConfig.getPort()));
		}

		// Server connection config
		raw.put(prefix + LDAPCommonProperties.FOLLOW_REFERRALS, String.valueOf(getFollowReferrals()));

		raw.put(prefix + LDAPCommonProperties.SEARCH_TIME_LIMIT, String.valueOf(getSearchTimeLimit()));

		raw.put(prefix + LDAPCommonProperties.SOCKET_TIMEOUT, String.valueOf(getSocketTimeout()));

		raw.put(prefix + LDAPCommonProperties.TLS_TRUST_ALL, String.valueOf(isTrustAllCerts()));
		raw.put(prefix + LDAPCommonProperties.RESULT_ENTRIES_LIMIT,
				String.valueOf(getResultEntriesLimit()));

		if (getClientTrustStore() != null)
		{
			raw.put(prefix + LDAPCommonProperties.TRUSTSTORE,
					String.valueOf(getClientTrustStore()));
		}
		
		if (getUsernameExtractorRegexp() != null)
		{
			raw.put(prefix + LDAPCommonProperties.USER_ID_EXTRACTOR_REGEXP,
					getUsernameExtractorRegexp());
		}
	}
	

	public void validateConfiguration(PKIManagement pkiMan) throws ConfigurationException
	{
		validateServersConfiguration();
		validateUserDNTemplate();
		validateValidUserFilter();
		validateClientTrustStore(pkiMan);
	}

	private void validateServersConfiguration() throws ConfigurationException
	{
		for (ServerSpecification conf : servers)
		{
			if (conf.getPort() > 65535 || conf.getPort() < 1)
			{
				throw new ConfigurationException("LDAP server port is out of range: " + conf.getPort());
			}
			if (!nonEmpty(conf.getServer()))
			{
				throw new ConfigurationException("LDAP server name is invalid: " + conf.getServer());
			}
		}
	}

	private void validateUserDNTemplate() throws ConfigurationException
	{
		if (nonEmpty(userDNTemplate) && !userDNTemplate.contains(USERNAME_TOKEN))
		{
			throw new ConfigurationException("DN template doesn't contain the mandatory token "
					+ USERNAME_TOKEN + ": " + userDNTemplate);
		}
	}

	private void validateValidUserFilter() throws ConfigurationException
	{
		if (validUserFilter != null)
		{
			try
			{
				Filter.create(validUserFilter);
			} catch (LDAPException e)
			{
				throw new ConfigurationException("Valid users filter is invalid.", e);
			}
		}
	}

	private void validateClientTrustStore(PKIManagement pkiMan) throws ConfigurationException
	{
		if (connectionMode != ConnectionMode.plain && !trustAllCerts)
		{
			try
			{
				pkiMan.getValidator(clientTrustStore);
			} catch (EngineException e)
			{
				throw new ConfigurationException("Invalid client truststore for the ldap client", e);
			}
		}
	}

	public String getSystemDN()
	{
		return systemDN;
	}

	public void setSystemDN(String systemDN)
	{
		this.systemDN = systemDN;
	}

	public String getSystemPassword()
	{
		return systemPassword;
	}

	public void setSystemPassword(String systemPassword)
	{
		this.systemPassword = systemPassword;
	}

	public String getValidUserFilter()
	{
		return validUserFilter;
	}

	public void setValidUserFilter(String validUserFilter)
	{
		this.validUserFilter = validUserFilter;
	}

	public String getUserDNTemplate()
	{
		return userDNTemplate;
	}

	public void setUserDNTemplate(String userDNTemplate)
	{
		this.userDNTemplate = userDNTemplate;
	}

	public ConnectionMode getConnectionMode()
	{
		return connectionMode;
	}

	public void setConnectionMode(ConnectionMode connectionMode)
	{
		this.connectionMode = connectionMode;
	}

	public List<ServerSpecification> getServers()
	{
		return servers;
	}

	public void setServers(List<ServerSpecification> servers)
	{
		this.servers = servers;
	}

	public int getFollowReferrals()
	{
		return followReferrals;
	}

	public void setFollowReferrals(int followReferrals)
	{
		this.followReferrals = followReferrals;
	}

	public int getSearchTimeLimit()
	{
		return searchTimeLimit;
	}

	public void setSearchTimeLimit(int searchTimeLimit)
	{
		this.searchTimeLimit = searchTimeLimit;
	}

	public int getSocketTimeout()
	{
		return socketTimeout;
	}

	public void setSocketTimeout(int socketTimeout)
	{
		this.socketTimeout = socketTimeout;
	}

	public boolean isTrustAllCerts()
	{
		return trustAllCerts;
	}

	public void setTrustAllCerts(boolean trustAllCerts)
	{
		this.trustAllCerts = trustAllCerts;
	}

	public String getClientTrustStore()
	{
		return clientTrustStore;
	}

	public void setClientTrustStore(String clientTrustStore)
	{
		this.clientTrustStore = clientTrustStore;
	}

	public String getUsernameExtractorRegexp()
	{
		return usernameExtractorRegexp;
	}

	public void setUsernameExtractorRegexp(String usernameExtractorRegexp)
	{
		this.usernameExtractorRegexp = usernameExtractorRegexp;
	}

	public UserDNResolving getUserDNResolving()
	{
		return userDNResolving;
	}

	public void setUserDNResolving(UserDNResolving userDNResolving)
	{
		this.userDNResolving = userDNResolving;
	}

	public String getLdapSearchBaseName()
	{
		return ldapSearchBaseName;
	}

	public void setLdapSearchBaseName(String ldapSearchBaseName)
	{
		this.ldapSearchBaseName = ldapSearchBaseName;
	}

	public String getLdapSearchFilter()
	{
		return ldapSearchFilter;
	}

	public void setLdapSearchFilter(String ldapSearchFilter)
	{
		this.ldapSearchFilter = ldapSearchFilter;
	}

	public SearchScope getLdapSearchScope()
	{
		return ldapSearchScope;
	}

	public void setLdapSearchScope(SearchScope ldapSearchScope)
	{
		this.ldapSearchScope = ldapSearchScope;
	}

	public int getResultEntriesLimit()
	{
		return resultEntriesLimit;
	}

	public void setResultEntriesLimit(int resultEntriesLimit)
	{
		this.resultEntriesLimit = resultEntriesLimit;
	}
}
