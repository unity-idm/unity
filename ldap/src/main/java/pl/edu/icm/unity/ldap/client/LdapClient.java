/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.client;

import static pl.edu.icm.unity.ldap.client.LdapUtils.nonEmpty;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.Logger;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ReadOnlySearchRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.security.canl.SSLContextCreator;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.ldap.client.LdapClientConfiguration.ConnectionMode;
import pl.edu.icm.unity.ldap.client.LdapProperties.BindAs;
import pl.edu.icm.unity.stdext.identity.X500Identity;

/**
 * LDAP v3 client code. Immutable -> thread safe.
 * <p>
 * Depending on configuration, it's possible to bind as normal user, or as a privileged (system) user
 * who is allowed to search a whole (sub-)tree
 * <p>
 * When binding as user, the code first binds with the provided username and password credentials. 
 * If this succeeds then (depending on configuration) user's attributes are retrieved and/or user's 
 * groups are assembled.
 * <p>
 * When binding as system user, the code first binds with the provided system username and password credentials.
 * The requested user is then searched. If found, the code checks the user password by attempting to
 * bind as the user. If this succeeds, the code again binds as system user, and proceeds to retrieve user
 * attributes as above. 
 * <p>
 * The attributes searching is pretty straightforward. The most of the code in this class is responsible for
 * flexible group retrieval. Both 'memberOf' style and 'member' means of expressing group membership are supported,
 * with some additional options. Most notably it is possible to use a full DN of the group or its attribute 
 * as the group name.  
 * 
 * @author K. Benedyczak
 */
public class LdapClient
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_LDAP, LdapClient.class);
	private static final org.apache.log4j.Logger legacyLog = 
			Log.getLegacyLogger(Log.U_SERVER_LDAP, LdapClient.class);

	private String idpName;
	private LdapGroupHelper groupHelper;
	
	public LdapClient(String idpName)
	{
		this.idpName = idpName;
		this.groupHelper = new LdapGroupHelper();
	}

	/**
	 * Performs authentication by binding and searches for all configured attributes.
	 * @param user
	 * @param password
	 * @param configuration
	 * @return
	 * @throws LDAPException
	 * @throws LdapAuthenticationException
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	public RemotelyAuthenticatedInput bindAndSearch(String userOrig, String password, 
			LdapClientConfiguration configuration) throws LDAPException, LdapAuthenticationException, 
			KeyManagementException, NoSuchAlgorithmException
	{
		LDAPConnection connection = createConnection(configuration);
		
		String user = LdapUtils.extractUsername(userOrig, configuration.getUserExtractPattern());
		
		String dn = establishUserDN(user, configuration, connection);
		log.debug("Established user's DN is: " + dn);
		
		bindAsUser(connection, dn, password, configuration);
		if (configuration.isBindOnly())
		{
			RemotelyAuthenticatedInput ret = new RemotelyAuthenticatedInput(idpName);
			ret.addIdentity(new RemoteIdentity(dn, X500Identity.ID));
			return ret;
		}
		
		if (configuration.getBindAs() == BindAs.system)
			bindAsSystem(connection, configuration);
		
		SearchResultEntry entry = findBaseEntry(configuration, dn, connection);
		
		RemotelyAuthenticatedInput ret = assembleBaseResult(entry);
		findGroupsMembership(connection, entry, configuration, ret.getGroups());
		
		performAdditionalQueries(connection, configuration, user, ret);
		ret.setRawAttributes(ret.getAttributes());
		
		connection.close();
		return ret;
	}

	/**
	 * Resolves information about a given user, with all the features, but without binding as the user 
	 * (so the user must be authenticated with other means). This works only when configured to bind as system.
	 * @param user
	 * @param configuration
	 * @return
	 * @throws LDAPException
	 * @throws LdapAuthenticationException
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 */
	public RemotelyAuthenticatedInput search(String userOrig, LdapClientConfiguration configuration) 
			throws LDAPException, LdapAuthenticationException, 
			KeyManagementException, NoSuchAlgorithmException
	{
		if (configuration.getBindAs() == BindAs.user)
		{
			log.error("LDAP verification of externaly verified credentials (as TLS verified certificates)"
					+ " can be only performed when the LDAP subsystem is configured to bind "
					+ "with a system credential");
			throw new LdapAuthenticationException("Can't authenticate");
		}
		
		String user = LdapUtils.extractUsername(userOrig, configuration.getUserExtractPattern());
		
		LDAPConnection connection = createConnection(configuration);
		
		String dn = establishUserDN(user, configuration, connection);
		log.debug("Established user's DN is: " + dn);
		
		if (configuration.isBindOnly())
		{
			RemotelyAuthenticatedInput ret = new RemotelyAuthenticatedInput(idpName);
			ret.addIdentity(new RemoteIdentity(dn, X500Identity.ID));
			return ret;
		}
		
		if (configuration.getBindAs() == BindAs.system)
			bindAsSystem(connection, configuration);
		
		SearchResultEntry entry = findBaseEntry(configuration, dn, connection);
		
		RemotelyAuthenticatedInput ret = assembleBaseResult(entry);
		findGroupsMembership(connection, entry, configuration, ret.getGroups());
		
		performAdditionalQueries(connection, configuration, user, ret);
		ret.setRawAttributes(ret.getAttributes());
		
		connection.close();
		return ret;
	}
	
	
	/**
	 * Returns DN of the user. Depending on configuration the user's DN can be simply formed from a 
	 * configured template or can be discovered with a custom search run as admin user. 
	 * @param username
	 * @param configuration
	 * @param connection
	 * @return
	 * @throws LDAPException
	 * @throws LdapAuthenticationException
	 */
	private String establishUserDN(String username, LdapClientConfiguration configuration, 
			LDAPConnection connection) throws LDAPException, LdapAuthenticationException
	{
		SearchSpecification searchForUser = configuration.getSearchForUserSpec(); 
		if (searchForUser == null)
			return configuration.getBindDN(username);

		bindAsSystem(connection, configuration);
		int timeLimit = configuration.getSearchTimeLimit();
		int sizeLimit = configuration.getAttributesLimit();
		DereferencePolicy derefPolicy = configuration.getDereferencePolicy();
		SearchResult result = performSearch(connection, searchForUser, username, 
				timeLimit, sizeLimit, derefPolicy);
		if (result.getEntryCount() == 0)
		{
			log.debug("Search for the user DN returned no results");
			throw new LdapAuthenticationException("User was not found");
		} else if (result.getEntryCount() > 1)
		{
			log.debug("Search for the user DN returned moe than one results");
			throw new LdapAuthenticationException("Too many users found");
		} else
		{
			return result.getSearchEntries().get(0).getDN();			
		}
	}
	
	/**
	 * Creates an ladp connection and secures it. Failover settings from configuration are taken into account.
	 * @param configuration
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws LDAPException
	 */
	private LDAPConnection createConnection(LdapClientConfiguration configuration) 
			throws KeyManagementException, NoSuchAlgorithmException, LDAPException
	{
		LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
		connectionOptions.setConnectTimeoutMillis(configuration.getSocketConnectTimeout());
		connectionOptions.setFollowReferrals(configuration.isFollowReferral());
		connectionOptions.setReferralHopLimit(configuration.getReferralHopCount());
		connectionOptions.setResponseTimeoutMillis(configuration.getSocketReadTimeout());
		
		FailoverServerSet failoverSet;
		if (configuration.getConnectionMode() == ConnectionMode.SSL)
		{
			X509CertChainValidator validator = configuration.getTlsValidator();
			SSLContext ctx = SSLContextCreator.createSSLContext(null, validator, 
					"TLS", "LDAP client", legacyLog);
			failoverSet = new FailoverServerSet(configuration.getServers(), 
					configuration.getPorts(), ctx.getSocketFactory(), connectionOptions);
		} else
		{
			failoverSet = new FailoverServerSet(configuration.getServers(), 
				configuration.getPorts(), connectionOptions);
		}
		
		LDAPConnection connection = failoverSet.getConnection();
		
		log.debug("Established connection to LDAP server");
		if (configuration.getConnectionMode() == ConnectionMode.startTLS)
		{
			X509CertChainValidator validator = configuration.getTlsValidator();
			SSLContext ctx = SSLContextCreator.createSSLContext(null, validator, 
					"TLSv1.2", "LDAP client", legacyLog);
			ExtendedResult extendedResult = connection.processExtendedOperation(
					new StartTLSExtendedRequest(ctx));

			if (extendedResult.getResultCode() != ResultCode.SUCCESS)
			{
				connection.close();
				throw new LDAPException(extendedResult.getResultCode(), "Unable to esablish " +
						"a secure TLS connection to the LDAP server: " + 
						extendedResult.toString());
			}
			log.debug("Connection upgraded to TLS");
		}
		return connection;
	}

	private void bindAsUser(LDAPConnection connection, String dn, String password, 
			LdapClientConfiguration configuration) throws LdapAuthenticationException, LDAPException
	{
		try
		{
			connection.bind(dn, password);
		} catch (LDAPException e)
		{
			if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode()))
			{
				log.debug("LDAP bind as user " + dn + " was not successful - invalid password");
				throw new LdapAuthenticationException("Wrong username or credentials", e);
			} else throw e;
		}
		log.debug("LDAP bind as user " + dn + " was successful");
	}

	private void bindAsSystem(LDAPConnection connection, LdapClientConfiguration configuration) 
			throws LdapAuthenticationException, LDAPException
	{
		String systemDN = configuration.getSystemDN();
		String systemPassword = configuration.getSystemPassword();
		try
		{
			connection.bind(systemDN, systemPassword);
		} catch (LDAPException e)
		{
			if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode()))
				throw new LdapAuthenticationException("Wrong username or credentials of the "
						+ "system LDAP client "
						+ "(system, not the ones provided by the user)", e);
			else
				throw e;
		}
		log.debug("LDAP bind as system user was successful");
	}

	private SearchResultEntry findBaseEntry(LdapClientConfiguration configuration, String dn,
			LDAPConnection connection) throws LdapAuthenticationException, LDAPException
	{
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
			throw new LdapAuthenticationException("User is not matching the valid users filter");
		return entry;
	}
	
	private RemotelyAuthenticatedInput assembleBaseResult(SearchResultEntry entry)
	{
		RemotelyAuthenticatedInput ret = new RemotelyAuthenticatedInput(idpName);
		for (Attribute a: entry.getAttributes())
		{
			ret.addAttribute(new RemoteAttribute(a.getName(), (Object[])a.getValues()));
		}
		ret.setRawAttributes(ret.getAttributes());
		ret.addIdentity(new RemoteIdentity(entry.getDN(), X500Identity.ID));
		return ret;
	}
	
	private void findGroupsMembership(LDAPConnection connection, SearchResultEntry userEntry,
			LdapClientConfiguration configuration, Map<String, RemoteGroupMembership> ret) 
					throws LDAPException
	{
		if (nonEmpty(configuration.getMemberOfAttribute()))
			groupHelper.findMemberOfGroups(ret, userEntry, configuration);
		
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
	private void searchGroupsForMember(LDAPConnection connection, Map<String, RemoteGroupMembership> ret,
			SearchResultEntry userEntry, LdapClientConfiguration configuration) throws LDAPException
	{
		String base = configuration.getGroupsBaseName();
		List<GroupSpecification> gss = configuration.getGroupSpecifications();
		Map<String, GroupSpecification> gsByObjectClass = new HashMap<>(gss.size());
		Set<String> attributes = new HashSet<>();
		attributes.add("objectClass");

		String searchFilter = groupHelper.buildGroupFilter(userEntry, gss, 
				configuration.isSearchGroupsInLdap());

		for (GroupSpecification gs: gss)
		{
			String oc = gs.getObjectClass();
			gsByObjectClass.put(oc, gs);
			if (nonEmpty(gs.getMemberAttribute()))
				attributes.add(gs.getMemberAttribute());
			if (nonEmpty(gs.getGroupNameAttribute()))
				attributes.add(gs.getGroupNameAttribute());
		}
		Filter filter;
		try
		{
			filter = Filter.create(searchFilter);
		} catch (LDAPException e)
		{
			throw new LDAPException(e.getResultCode(), 
					"Specification of group object class is wrong. Unable to create a filter, " +
					"which was: " + searchFilter, e);
		}

		if (log.isDebugEnabled())
			log.debug("Will search groups, from base "+ base + " with filter " + searchFilter +
					" collecting attributes " + attributes);
		
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
					groupHelper.findMemberInGroup(ret, userEntry, groupEntry, gs);
					break;
				}
			}
		}
	}	

	
	private void performAdditionalQueries(LDAPConnection connection, LdapClientConfiguration configuration, 
			String user, RemotelyAuthenticatedInput principalData) throws LDAPException
	{
		int timeLimit = configuration.getSearchTimeLimit();
		int sizeLimit = configuration.getAttributesLimit();
		DereferencePolicy derefPolicy = configuration.getDereferencePolicy();

		List<SearchSpecification> searchSpecs = configuration.getExtraSearches();
		for (SearchSpecification searchSpec: searchSpecs)
		{
			SearchResult result = performSearch(connection, searchSpec, user, 
					timeLimit, sizeLimit, derefPolicy);
			consolidateAttributes(result, principalData);
		}
	}
	
	private SearchResult performSearch(LDAPConnection connection, SearchSpecification searchSpec,
			String username, int timeLimit, int sizeLimit, DereferencePolicy derefPolicy) throws LDAPException
	{
		String[] queriedAttributes = searchSpec.getAttributes();
		Filter validUsersFilter = searchSpec.getFilter(username);
		String base = searchSpec.getBaseDN(username);
		SearchScope scope = searchSpec.getScope().getInternalScope();
		log.debug("Performing LDAP search filter: [" + validUsersFilter + "] base: [" + base + 
				"] scope: [" + scope +"]");
		ReadOnlySearchRequest searchRequest = new SearchRequest(base, scope, derefPolicy, 
				sizeLimit, timeLimit, false, validUsersFilter, queriedAttributes);
		return connection.search(searchRequest);
	}
	
	private void consolidateAttributes(SearchResult result, RemotelyAuthenticatedInput principalData)
	{
		Map<String, Set<String>> attributes = new HashMap<String, Set<String>>();
		for (SearchResultEntry entry: result.getSearchEntries())
		{
			for (Attribute a: entry.getAttributes())
			{
				Set<String> curValues = attributes.get(a.getName());
				if (curValues == null)
				{
					curValues = new LinkedHashSet<String>();
					attributes.put(a.getName(), curValues);
				}
				Collections.addAll(curValues, a.getValues());
			}
		}
		
		for (Map.Entry<String, Set<String>> e: attributes.entrySet())
		{
			principalData.addAttribute(new RemoteAttribute(e.getKey(), e.getValue().toArray()));
		}
	}
}



