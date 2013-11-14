/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.ReadOnlySearchRequest;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

import eu.emi.security.authn.x509.impl.X500NameUtils;

import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.utils.Log;

/**
 * LDAP v3 client code. Immutable -> thread safe.
 * <p>
 * The code first binds with the provided username and password credentials. If this succeeds then 
 * (depending on configuration) user's attributes are retrieved and/or user's groups are assembled.
 * <p>
 * The attributes searching is pretty straightforward. The most of the code in this class is responsible for
 * flexible group retrieval. Both 'memberOf' style and 'member' means of expressing group membership are supported,
 * with some additional options. Most notably it is possible to use a full DN of the group or its attribute 
 * as the group name.  
 * 
 * TODO - ssl connection mode.
 * @author K. Benedyczak
 */
public class LdapClient
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, LdapClient.class);
	public static final String ORIGINAL_GROUP_NAME = "originalGroupName";
	private String idpName;
	
	public LdapClient(String idpName)
	{
		this.idpName = idpName;
	}

	public RemotelyAuthenticatedInput bindAndSearch(String user, String password, 
			LdapClientConfiguration configuration) throws LDAPException
	{
		FailoverServerSet failoverSet = new FailoverServerSet(configuration.getServers(), 
				configuration.getPorts());
		LDAPConnection connection = failoverSet.getConnection();
		
		String dn = configuration.getBindDN(user);
		//TODO handle authn error so it can be easily distinguished in the calling code
		connection.bind(dn, password);
		
		if (configuration.isBindOnly())
		{
			RemotelyAuthenticatedInput ret = new RemotelyAuthenticatedInput(idpName);
			List<RemoteIdentity> identities = ret.getIdentities();
			identities.add(new RemoteIdentity(dn));
			return ret;
		}
		
		String[] queriedAttributes = configuration.getQueriedAttributes();
		SearchScope searchScope = configuration.getSearchScope();
		
		int timeLimit = configuration.getSearchTimeLimit();
		int sizeLimit = configuration.getAttributesLimit();
		DereferencePolicy derefPolicy = configuration.getDereferencePolicy();
		Filter validUsersFilter = configuration.getValidUsersFilter();
		
		ReadOnlySearchRequest searchRequest = new SearchRequest(dn, searchScope, derefPolicy, 
				sizeLimit, timeLimit, false, validUsersFilter, queriedAttributes);
		SearchResult result = connection.search(searchRequest);
		
		SearchResultEntry entry = result.getSearchEntry(dn);
		if (entry == null)
			; //TODO - the same case as authn error - the user was not returned in the search,
			// what means that it was filtered out by the valid users filter.
		
		RemotelyAuthenticatedInput ret = assembleBaseResult(entry);
		findGroupsMembership(connection, entry, configuration, ret.getGroups());
		
		return ret;
		
	}
	
	private RemotelyAuthenticatedInput assembleBaseResult(SearchResultEntry entry)
	{
		RemotelyAuthenticatedInput ret = new RemotelyAuthenticatedInput(idpName);
		List<RemoteAttribute> attributes = ret.getAttributes();
		for (Attribute a: entry.getAttributes())
		{
			attributes.add(new RemoteAttribute(a.getBaseName(), (Object[])a.getValues()));
		}
		
		List<RemoteIdentity> identities = ret.getIdentities();
		identities.add(new RemoteIdentity(entry.getDN()));
		return ret;
	}
	
	private void findGroupsMembership(LDAPConnection connection, SearchResultEntry userEntry,
			LdapClientConfiguration configuration, List<RemoteGroupMembership> ret) 
					throws LDAPException
	{
		if (nonEmpty(configuration.getMemberOfAttribute()))
			findMemberOfGroups(ret, userEntry, configuration);
		
		if (nonEmpty(configuration.getGroupsBaseName()))
			searchGroupsForMember(connection, ret, userEntry, configuration);
	}
	
	/**
	 * Complex variant: groups needs to be retrieved and searched for the user being a member.
	 * @param connection
	 * @param ret
	 * @param userEntry
	 * @param configuration
	 * @throws LDAPException 
	 */
	private void searchGroupsForMember(LDAPConnection connection, List<RemoteGroupMembership> ret,
			SearchResultEntry userEntry, LdapClientConfiguration configuration) throws LDAPException
	{
		String base = configuration.getGroupsBaseName();
		List<GroupSpecification> gss = configuration.getGroupSpecifications();
		Map<String, GroupSpecification> gsByObjectClass = new HashMap<>(gss.size());
		StringBuilder searchFilter = new StringBuilder(128);
		searchFilter.append("|(");
		Set<String> attributes = new HashSet<>();
		attributes.add("objectClass");

		for (GroupSpecification gs: gss)
		{
			String oc = gs.getObjectClass();
			searchFilter.append("(objectClass=").append(oc).append(")");
			gsByObjectClass.put(oc, gs);
			if (nonEmpty(gs.getMemberAttribute()))
				attributes.add(gs.getMemberAttribute());
			if (nonEmpty(gs.getGroupNameAttribute()))
				attributes.add(gs.getGroupNameAttribute());
		}
		searchFilter.append(")");
		Filter filter;
		try
		{
			filter = Filter.create(searchFilter.toString());
		} catch (LDAPException e)
		{
			throw new LDAPException(e.getResultCode(), 
					"Specification of group object class is wrong. Unable to create a filter, " +
					"which was: " + searchFilter, e);
		}
		ReadOnlySearchRequest searchRequest = new SearchRequest(base, SearchScope.SUB, 
					configuration.getDereferencePolicy(), 
					configuration.getAttributesLimit(), configuration.getSearchTimeLimit(), 
					false, filter, attributes.toArray(new String[attributes.size()]));
		SearchResult result = connection.search(searchRequest);
		
		for (SearchResultEntry groupEntry: result.getSearchEntries())
		{
			String[] classes = groupEntry.getObjectClassValues();
			for (String clazz: classes)
			{
				GroupSpecification gs = gsByObjectClass.get(clazz);
				if (gs != null)
				{
					findMemberInGroup(ret, userEntry, groupEntry, gs);
					break;
				}
			}
		}
	}	

	private void findMemberInGroup(List<RemoteGroupMembership> ret, SearchResultEntry userEntry, 
			SearchResultEntry groupEntry, GroupSpecification gs)
	{
		String memberAttribute = gs.getMemberAttribute();
		Attribute membersA = groupEntry.getAttribute(memberAttribute);
		if (membersA == null)
			return;
		String[] members = membersA.getValues();
		if (nonEmpty(gs.getMatchByMemberAttribute()))
		{
			String matchAttributeName = gs.getMatchByMemberAttribute();
			Attribute matchAttribute = userEntry.getAttribute(matchAttributeName);
			if (matchAttribute == null)
				return;
			String matchValue = matchAttribute.getValue();
			if (matchValue == null)
				return;
			
			for (String m: members)
			{
				String name = extractNameFromDn(matchAttributeName, m);
				if (name != null && name.equals(matchValue))
				{
					RemoteGroupMembership gm = createGroupMembership(groupEntry.getDN(), 
							gs.getGroupNameAttribute()); 
					if (gm != null)
						ret.add(gm);
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
						ret.add(gm);
					break;
				}
			}
		}
	}

	/**
	 * Simple variant: user's attributes contain information about user's group membership.
	 * @param ret
	 * @param userEntry
	 * @param configuration
	 */
	private void findMemberOfGroups(List<RemoteGroupMembership> ret,
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
					ret.add(gm);
			}
		}
	}
	
	private RemoteGroupMembership createGroupMembership(String groupDn, String memberOfGroupAttr)
	{
		String groupName = nonEmpty(memberOfGroupAttr) ? extractNameFromDn(memberOfGroupAttr, groupDn) : groupDn;
		if (groupName == null)
			return null;
		RemoteGroupMembership rg = new RemoteGroupMembership(groupName);
		rg.getMetadata().put(ORIGINAL_GROUP_NAME, groupDn);
		return rg;
	}
	
	
	/**
	 * Returns a value of the nameAttribute in dn. If not found then null is returned. This is intended 
	 * as in the code using this method this is a normal, not exceptional condition.
	 * @param nameAttribute
	 * @param dn
	 * @return
	 * @throws LDAPException if the dn is invalid
	 */
	private static String extractNameFromDn(String nameAttribute, String dn)
	{
		RDN[] rdns;
		try
		{
			rdns = DN.getRDNs(nameAttribute);
		} catch (LDAPException e)
		{
			log.warn("Found a string which is not a DN, what was expected. Most probably the LDAP " +
					"configuration is invalid wrt the schema used by the LDAP server. " +
					"Expected as DN: " + dn, e);
			return null;
		}
		for (RDN rdn: rdns)
		{
			String[] attrNames = rdn.getAttributeNames();
			String[] attrValues = rdn.getAttributeValues();
			if (attrNames.length == 1 && attrValues.length == 1 && attrNames[0].equals(nameAttribute))
			{
				return attrValues[0];
			}
		}
		return null;
	}
	
	private static boolean nonEmpty(String a)
	{
		return a != null && !a.isEmpty();
	}
}



