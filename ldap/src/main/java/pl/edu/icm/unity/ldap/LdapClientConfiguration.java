/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;

import eu.unicore.util.configuration.ConfigurationException;

/**
 * Manages configuration of the LDAP client.
 * @author K. Benedyczak
 */
public class LdapClientConfiguration
{
	private LdapProperties ldapProperties;
	
	public static final String USERNAME_TOKEN = "{USERNAME}";
	
	private String[] servers;
	private int[] ports;
	private String[] queriedAttributes;
	private String bindTemplate;
	private List<GroupSpecification> groups;
	private Filter validUsersFilter;
	
	public LdapClientConfiguration(LdapProperties ldapProperties)
	{
		this.ldapProperties = ldapProperties;
		List<String> servers = ldapProperties.getListOfValues(LdapProperties.SERVERS);
		this.servers = servers.toArray(new String[servers.size()]);

		List<String> ports = ldapProperties.getListOfValues(LdapProperties.PORTS);
		this.ports = new int[ports.size()];
		for (int i=0; i<ports.size(); i++)
			try
			{
				this.ports[i] = Integer.parseInt(ports.get(i));
				if (this.ports[i] > 65535 || this.ports[i] < 1)
					throw new ConfigurationException("LDAP server port is out of range: " + ports.get(i));
			} catch (NumberFormatException e)
			{
				throw new ConfigurationException("LDAP server port is not a number: " + ports.get(i));
			}

		if (this.servers.length != this.ports.length)
			throw new ConfigurationException("LDAP server ports number is not equal the number of servers.");
		
		List<String> attributes = ldapProperties.getListOfValues(LdapProperties.ATTRIBUTES);
		queriedAttributes = attributes.toArray(new String[attributes.size()]);
		
		bindTemplate = ldapProperties.getValue(LdapProperties.USER_DN_TEMPLATE);
		if (!bindTemplate.contains(USERNAME_TOKEN))
			throw new ConfigurationException("DN template doesn't contain the mandatory token " + 
					USERNAME_TOKEN + ": " + bindTemplate);
		
		Set<String> keys = ldapProperties.getStructuredListKeys(LdapProperties.GROUP_DEFINITION_PFX);
		groups = new ArrayList<>(keys.size());
		for (String key: keys)
		{
			GroupSpecification gs = new GroupSpecification();
			gs.setGroupNameAttribute(ldapProperties.getValue(key+LdapProperties.GROUP_DEFINITION_NAME_ATTR));
			gs.setMatchByMemberAttribute(ldapProperties.getValue(
					key+LdapProperties.GROUP_DEFINITION_MATCHBY_MEMBER_ATTR));
			gs.setMemberAttribute(ldapProperties.getValue(key + LdapProperties.GROUP_DEFINITION_MEMBER_ATTR));
			gs.setObjectClass(ldapProperties.getValue(key + LdapProperties.GROUP_DEFINITION_OC));
			groups.add(gs);
		}
		
		try
		{
			String filterStr = ldapProperties.getValue(LdapProperties.VALID_USERS_FILTER);
			validUsersFilter = filterStr == null ? Filter.create("objectclass=*") : Filter.create(filterStr);
		} catch (LDAPException e)
		{
			throw new ConfigurationException("Valid users filter is invalid.", e);
		}
	}

	public String[] getServers()
	{
		return servers;
	}
	
	public int[] getPorts()
	{
		return ports;
	}
	
	public String getBindDN(String userName)
	{
		return bindTemplate.replace(USERNAME_TOKEN, userName);
	}
	
	public boolean isBindOnly()
	{
		return ldapProperties.getBooleanValue(LdapProperties.BIND_ONLY);
	}
	
	public String[] getQueriedAttributes()
	{
		return queriedAttributes;
	}
	
	public SearchScope getSearchScope()
	{
		return SearchScope.SUB;
	}
	
	public int getSearchTimeLimit()
	{
		return ldapProperties.getIntValue(LdapProperties.SEARCH_TIME_LIMIT);
	}
	
	public int getAttributesLimit()
	{
		return 1000;
	}
	
	public DereferencePolicy getDereferencePolicy()
	{
		return DereferencePolicy.ALWAYS;
	}
	
	public Filter getValidUsersFilter()
	{
		return validUsersFilter;
	}
	
	public String getGroupsBaseName()
	{
		return ldapProperties.getValue(LdapProperties.GROUPS_BASE_NAME);
	}
	
	public List<GroupSpecification> getGroupSpecifications()
	{
		return groups;
	}
	
	public String getMemberOfAttribute()
	{
		return ldapProperties.getValue(LdapProperties.MEMBER_OF_ATTRIBUTE);		
	}
	
	public String getMemberOfGroupAttribute()
	{
		return ldapProperties.getValue(LdapProperties.MEMBER_OF_GROUP_ATTRIBUTE);
	}
	
	public int getSocketConnectTimeout()
	{
		return ldapProperties.getIntValue(LdapProperties.SOCKET_TIMEOUT);
	}

	public int getSocketReadTimeout()
	{
		return ldapProperties.getIntValue(LdapProperties.SOCKET_TIMEOUT);
	}
	
	public int getReferralHopCount()
	{
		return ldapProperties.getIntValue(LdapProperties.FOLLOW_REFERRALS);
	}
	
	public boolean isFollowReferral()
	{
		 return getReferralHopCount() == 0;
	}

}
