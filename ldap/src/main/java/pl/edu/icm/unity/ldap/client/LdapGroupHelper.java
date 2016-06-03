/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import static pl.edu.icm.unity.ldap.client.LdapUtils.extractNameFromDn;
import static pl.edu.icm.unity.ldap.client.LdapUtils.nonEmpty;

import java.util.List;
import java.util.Map;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.SearchResultEntry;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;

/**
 * Helper immutable object with utility methods allowing for creation of group search filters
 * and searching users in LDAP groups. This code is not talking to LDAP - it either prepares query filters
 * or postprocess returned data.
 *  
 * @author K. Benedyczak
 */
public class LdapGroupHelper
{
	public static final String ORIGINAL_GROUP_NAME = "originalGroupName";
	
	/**
	 * Builds a complete LDAP filter which either will return all group objects with given membership attributes
	 * or additionally adds filtering so only groups where user is member are returned. 
	 * @param userEntry
	 * @param gss
	 * @param searchByLdap
	 * @return
	 */
	public String buildGroupFilter(SearchResultEntry userEntry, List<GroupSpecification> gss, 
			boolean searchByLdap)
	{
		StringBuilder searchFilter = new StringBuilder(512);
		searchFilter.append("(|");
		for (GroupSpecification gs: gss)
		{
			searchFilter.append( searchByLdap ? buildCompleteGroupFilter(userEntry, gs) : 
				buildSimpleGroupFilter(gs));
				
		}
		searchFilter.append(")");
		return searchFilter.toString();
	}
	
	private String buildCompleteGroupFilter(SearchResultEntry userEntry, GroupSpecification gs)
	{
		if (!nonEmpty(gs.getMemberAttribute()))
			return buildSimpleGroupFilter(gs);
		
		String userFilter;
		if (nonEmpty(gs.getMatchByMemberAttribute()))
		{
			String matchValue = getUsersGroupMatchAttributeValue(userEntry, gs);
			userFilter = gs.getMemberAttribute() + "=" + matchValue;
		} else 
		{
			userFilter = gs.getMemberAttribute() + "=" + userEntry.getDN();
		}
		
		StringBuilder searchFilter = new StringBuilder(512);
		String oc = gs.getObjectClass();
		searchFilter.append("(&").
			append("(objectClass=").append(oc).append(")").
			append("(").append(userFilter).append(")").
			append(")");
		return searchFilter.toString();
	}

	private String buildSimpleGroupFilter(GroupSpecification gs)
	{
		String oc = gs.getObjectClass();
		return "(objectClass=" + oc + ")";
	}

	
	/**
	 * Searches for a member in a given search result entry. If found the member is added the map with
	 * returned membership.
	 * @param ret
	 * @param userEntry
	 * @param groupEntry
	 * @param gs
	 */
	public void findMemberInGroup(Map<String, RemoteGroupMembership> ret, SearchResultEntry userEntry, 
			SearchResultEntry groupEntry, GroupSpecification gs)
	{
		String memberAttribute = gs.getMemberAttribute();
		Attribute membersA = groupEntry.getAttribute(memberAttribute);
		if (membersA == null)
			return;
		String[] members = membersA.getValues();
		if (nonEmpty(gs.getMatchByMemberAttribute()))
		{
			String matchValue = getUsersGroupMatchAttributeValue(userEntry, gs);
			if (matchValue == null)
				return;
			
			for (String m: members)
			{
				if (m.equals(matchValue))
				{
					RemoteGroupMembership gm = createGroupMembership(groupEntry.getDN(), 
							gs.getGroupNameAttribute()); 
					if (gm != null)
						ret.put(gm.getName(), gm);
					break;
				}
			}			
		} else
		{
			for (String m: members)
			{
				if (X500NameUtils.equal(m, userEntry.getDN()))
				{
					RemoteGroupMembership gm = createGroupMembership(groupEntry.getDN(), 
							gs.getGroupNameAttribute()); 
					if (gm != null)
						ret.put(gm.getName(), gm);
					break;
				}
			}
		}
	}

	private String getUsersGroupMatchAttributeValue(SearchResultEntry userEntry, GroupSpecification gs)
	{
		String matchAttributeName = gs.getMatchByMemberAttribute();
		Attribute matchAttribute = userEntry.getAttribute(matchAttributeName);
		if (matchAttribute == null)
			return null;
		return matchAttribute.getValue();
	}
	
	/**
	 * Simple variant: user's attributes contain information about user's group membership.
	 * @param ret
	 * @param userEntry
	 * @param configuration
	 */
	public void findMemberOfGroups(Map<String, RemoteGroupMembership> ret,
			SearchResultEntry userEntry, LdapClientConfiguration configuration)
	{
		Attribute ga = userEntry.getAttribute(configuration.getMemberOfAttribute());
		if (ga != null)
		{
			String[] groups = ga.getValues();
			String memberOfGroupAttr = configuration.getMemberOfGroupAttribute();
			for (int i=0; i<groups.length; i++)
			{
				RemoteGroupMembership gm = createGroupMembership(groups[i], memberOfGroupAttr); 
				if (gm != null)
					ret.put(gm.getName(), gm);
			}
		}
	}
	
	private RemoteGroupMembership createGroupMembership(String groupDn, String memberOfGroupAttr)
	{
		String groupName = nonEmpty(memberOfGroupAttr) ? 
				extractNameFromDn(memberOfGroupAttr, groupDn) : groupDn;
		if (groupName == null)
			return null;
		RemoteGroupMembership rg = new RemoteGroupMembership(groupName);
		rg.getMetadata().put(ORIGINAL_GROUP_NAME, groupDn);
		return rg;
	}
}
