/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.unboundid.ldap.sdk.CompareRequest;
import com.unboundid.ldap.sdk.CompareResult;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.ReadOnlySearchRequest;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.ldap.client.LdapAuthenticationException;
import pl.edu.icm.unity.ldap.client.LdapClient;
import pl.edu.icm.unity.ldap.client.LdapClientConfiguration;
import pl.edu.icm.unity.ldap.client.LdapProperties;
import pl.edu.icm.unity.ldap.endpoint.LdapEndpointFactory;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.confirmation.ConfirmationInfo;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

public class LdapEndpointTests extends DBIntegrationTestBase
{
	@Autowired
	private AuthenticatorManagement authnMan;
	public static final String ldapEndpointHostname = "127.0.0.1";
	public static final int ldapEndpointPort = 1389;
	public static final String ldapEndpointDNTemplate = "cn={USERNAME},ou=system";

	private String credentialId = "credential1";
	private String username1 = "clarin";
	private String apass1 = "heslo";
	private String email1 = "clarin@email.com";

	public static final String ldapEndpointConfiguration = "unity.ldapServer.host="
			+ ldapEndpointHostname + "\n" + "unity.ldapServer.ldapPort="
			+ ldapEndpointPort + "\n" + "unity.ldapServer.ldapsPort=1636\n"
			+ "unity.ldapServer.groupMember=member\n"
			+ "unity.ldapServer.groupMemberDnRegexp=ou=groups\n"
			+ "unity.ldapServer.returnedUserAttributes=cn,entryDN,jpegPhoto\n"
			+ "unity.ldapServer.userNameAliases=cn,mail\n"
			+ "unity.ldapServer.tls=true\n"
			+ "unity.ldapServer.groupOfNamesReturnFormat=cn=%s,ou=happygroups\n"
			+ "unity.ldapServer.certPassword=test.p4ss\n"
			+ "unity.ldapServer.keystoreName=ldap.test.keystore\n";

	private static final String ldapClientConfiguration = "ldap.servers.1=%s\n"
			+ "ldap.ports.1=%d\n" + "ldap.userDNTemplate=%s\n"
			+ "ldap.trustAllServerCertificates=true\n"
			+ "ldap.translationProfile=dummy\n";

	public static LdapClientConfiguration getLdapClientConfig() throws IOException
	{
		return getLdapClientConfig(null, null, 0, null, null);
	}

	public static LdapClientConfiguration getLdapClientConfig(String appendix)
			throws IOException
	{
		return getLdapClientConfig(null, null, 0, null, appendix);
	}

	public static LdapClientConfiguration getLdapClientConfig(String baseconf, String hostname,
			int port, String dnTemplate, String appendix) throws IOException
	{
		if (null == baseconf)
		{
			baseconf = ldapClientConfiguration;
		}
		if (null == hostname)
		{
			hostname = ldapEndpointHostname;
		}
		if (0 == port)
		{
			port = ldapEndpointPort;
		}
		if (null == dnTemplate)
		{
			dnTemplate = ldapEndpointDNTemplate;
		}
		if (null != appendix)
		{
			baseconf += appendix;
		}
		baseconf = String.format(baseconf, hostname, port, dnTemplate);

		Properties p = new Properties();
		p.load(new StringReader(baseconf));
		LdapProperties lp = new LdapProperties(p);
		return new LdapClientConfiguration(lp, null);
	}

	@Before
	public void setUp() throws Exception
	{
		// do the magic initialisation
		//
		setupPasswordAuthn();
		String ldap_typeId = "password with ldap-simple";

		final String AUTHENTICATOR_PASS = "ApassLDAPT";
		authnMan.createAuthenticator(AUTHENTICATOR_PASS, ldap_typeId, null, "",
				credentialId);
		List<AuthenticationOptionDescription> authnCfg = new ArrayList<>();
		authnCfg.add(new AuthenticationOptionDescription(AUTHENTICATOR_PASS));

		// create a simple test user
		setUpUser(username1, apass1, email1);

		// deploy
		//
		EndpointConfiguration cfg = new EndpointConfiguration(
				new I18nString("ldapEndpoint"), "desc", authnCfg,
				ldapEndpointConfiguration, "defaultRealm");
		endpointMan.deploy(LdapEndpointFactory.NAME, "ldapEndpoint", "/mock", cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());
	}

	private void setUpUser(String username, String apass, String email) throws EngineException
	{
		setUpUser(username, apass, email, AuthorizationManagerImpl.USER_ROLE);
	}

	private void setUpUser(String username, String apass, String email, String role)
			throws EngineException
	{
		// create a simple test user
		//
		IdentityParam user_id_param = new IdentityParam(UsernameIdentity.ID, username);

		Identity user_id = idsMan.addEntity(user_id_param, CRED_REQ_PASS, EntityState.valid,
				false);
		eCredMan.setEntityCredential(new EntityParam(user_id), credentialId,
				new PasswordToken(apass).toJson());
		Attribute sa = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, "/",
				role);
		attrsMan.createAttribute(new EntityParam(user_id), sa);

		if (null == email)
		{
			return;
		}

		IdentityParam email_id_param = new IdentityParam(EmailIdentity.ID, email);
		// this is required otherwise the email won't be returned
		// in IdentityResolverImpl::getEntity
		email_id_param.setConfirmationInfo(new ConfirmationInfo(true));
		Identity email_id = idsMan.addEntity(email_id_param, CRED_REQ_PASS,
				EntityState.valid, false);
		eCredMan.setEntityCredential(new EntityParam(email_id), credentialId,
				new PasswordToken(apass).toJson());

		// merge identities email + username
		idsMan.mergeEntities(new EntityParam(user_id), new EntityParam(email_id), false);
	}

	@Test
	public void testBind() throws Exception
	{
		LdapClient client = new LdapClient("test");

		// test binding that should SUCCEED
		for (String s : new String[] {
				"ldap.connectionMode=plain\n" + "ldap.authenticateOnly=true",
				"ldap.connectionMode=startTLS\n" + "ldap.authenticateOnly=true", })
		{
			client.bindAndSearch(username1, apass1, getLdapClientConfig(s));
		}

		// test binding that should FAIL
		try
		{
			client.bindAndSearch("user1", "wrong", getLdapClientConfig());
			fail("authenticated with a wrong password");
		} catch (LdapAuthenticationException e)
		{
			// ok, expected
		}
	}

	@Test
	public void testLdapApi() throws Exception
	{
		// the goal here is to test
		// - bind + get DN + limited search
		//
		String extended_conf = "" + "ldap.servers.1=%s\n" + "ldap.ports.1=%d\n"
				+ "ldap.trustAllServerCertificates=true\n"
				+ "ldap.translationProfile=dummy\n" + "ldap.userDNSearchKey=s1\n"
				+ "ldap.bindAs=system\n"
				+ String.format("ldap.systemDN=cn=%s,ou=system\n", username1)
				+ String.format("ldap.systemPassword=%s\n", apass1)
				+ String.format("ldap.additionalSearch.s1.filter=(&(mail=%s)(objectClass=person))\n",
						email1)
				+ String.format("ldap.additionalSearch.s1.baseName=cn=%s,ou=system\n",
						username1)
				+ "ldap.additionalSearch.s1.selectedAttributes=dn,mail";
		LdapClientConfiguration ldapConfig = getLdapClientConfig(extended_conf, null, 0,
				null, null);

		// test LDAP connection via LdapClient
		LdapClient client = new LdapClient("test");
		client.bindAndExecute(username1, apass1, ldapConfig, (connInfo) -> {
			try
			{
				String[] queriedAttributes = ldapConfig.getQueriedAttributes();
				SearchScope searchScope = ldapConfig.getSearchScope();

				int timeLimit = ldapConfig.getSearchTimeLimit();
				int sizeLimit = ldapConfig.getAttributesLimit();
				DereferencePolicy derefPolicy = ldapConfig.getDereferencePolicy();
				Filter validUsersFilter = ldapConfig.getValidUsersFilter();
				// bind and search uses unsupported operations -
				// search over arbitrary user attributes
				// therefore, hardcode dn to something Unity's
				// LDAP server supports in general
				String dn = "ou=system";
				ReadOnlySearchRequest searchRequest = new SearchRequest(dn,
						searchScope, derefPolicy, sizeLimit, timeLimit,
						false, validUsersFilter, queriedAttributes);
				SearchResult result = connInfo.connection.search(searchRequest);
				SearchResultEntry entry = result.getSearchEntry(dn);
				assertNotNull(entry);
			} catch (Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		});
	}

	@Test
	public void testMultipleConnections() throws Exception
	{
		// the goal here is to test
		// - bind + get DN + limited search
		//
		String extended_conf = "" + "ldap.authenticateOnly=true\n";
		LdapClientConfiguration ldapConfig1 = getLdapClientConfig(extended_conf);
		LdapClientConfiguration ldapConfig2 = getLdapClientConfig(null, null, 0,
				"mail={USERNAME},ou=system", extended_conf);

		// test LDAP connection via LdapClient
		LdapClient client = new LdapClient("test");
		final int iteration_count = 50;

		System.out.println(String.format("testing [%d] auth. of two different users...",
				iteration_count));
		String username2 = "striga";
		String apass2 = "skareda";
		setUpUser(username2, apass2, null);
		for (int i = 0; i < iteration_count; ++i)
		{
			client.bindAndSearch(username1, apass1, ldapConfig1);
			client.bindAndSearch(username2, apass2, ldapConfig1);
		}

		System.out.println(String.format("testing [%d] auth. of the same user...",
				iteration_count));
		for (int i = 0; i < iteration_count; ++i)
		{
			client.bindAndSearch(email1, apass1, ldapConfig2);
			client.bindAndSearch(username1, apass1, ldapConfig1);
			try
			{
				client.bindAndSearch(username1, "b", ldapConfig1);
				fail("should have failed!");
			} catch (LdapAuthenticationException e)
			{
			}
		}
	}

	@Test
	public void testConcurrency() throws Exception
	{
		LdapClientConfiguration ldapConfig1 = getLdapClientConfig(
				"ldap.authenticateOnly=true\n");
		LdapClientConfiguration ldapConfig2 = getLdapClientConfig(
				"ldap.authenticateOnly=false\n");

		String username2 = "conc2";
		String apass2 = "concpass2";
		setUpUser(username2, apass2, null, AuthorizationManagerImpl.SYSTEM_MANAGER_ROLE);

		// test LDAP connection via LdapClient
		LdapClient client2 = new LdapClient("test2");
		client2.bindAndExecute(username2, apass2, ldapConfig2, (connInfo2) -> {

			try
			{
				// we are in an active LDAP session with
				// username1 authenticated
				// - start another session with username 2 and
				// close it
				LdapClient client1 = new LdapClient("test1");
				client1.bindAndSearch(username1, apass1, ldapConfig1);

				// we overwrote (if there is a problem) the
				// invocation context with username2
				// so test using client1
				String[] queriedAttributes = new String[] { "*" };
				SearchScope searchScope = ldapConfig2.getSearchScope();
				int timeLimit = ldapConfig2.getSearchTimeLimit();
				int sizeLimit = ldapConfig2.getAttributesLimit();
				DereferencePolicy derefPolicy = ldapConfig2.getDereferencePolicy();
				String dn = String.format("cn=%s", username2);
				Filter validUsersFilter = Filter.create(String.format("(%s)", dn));
				ReadOnlySearchRequest searchRequest = new SearchRequest(dn,
						searchScope, derefPolicy, sizeLimit, timeLimit,
						false, validUsersFilter, queriedAttributes);
				SearchResult result = connInfo2.connection.search(searchRequest);
				result.getSearchEntry(dn);
			} catch (Exception e)
			{
				e.printStackTrace();
				assertTrue(false);
			}
		});
	}

	@Test
	public void testGroups() throws Exception
	{
		// the goal here is to test
		// - bind + get DN + limited search
		//
		String extended_conf = "" + "ldap.servers.1=%s\n" + "ldap.ports.1=%d\n"
				+ "ldap.trustAllServerCertificates=true\n"
				+ "ldap.translationProfile=dummy\n" + "ldap.userDNSearchKey=s1\n"
				+ "ldap.bindAs=system\n"
				+ String.format("ldap.systemDN=cn=%s,ou=system\n", username1)
				+ String.format("ldap.systemPassword=%s\n", apass1)
				+ String.format("ldap.additionalSearch.s1.filter=(&(mail=%s)(objectClass=person))\n",
						email1)
				+ String.format("ldap.additionalSearch.s1.baseName=cn=%s,ou=system\n",
						username1)
				+ "ldap.additionalSearch.s1.selectedAttributes=dn,mail";
		LdapClientConfiguration ldapConfig = getLdapClientConfig(extended_conf, null, 0,
				null, null);

		// test LDAP connection via LdapClient
		LdapClient client = new LdapClient("test");
		client.bindAndExecute(username1, apass1, ldapConfig, (connInfo) -> {
			try
			{
				{
					String dn = "cn=/,ou=groups";
					CompareRequest req = new CompareRequest(dn, "member", String
							.format("cn=%s,ou=system", username1));
					CompareResult result = connInfo.connection.compare(req);
					assertTrue(result.compareMatched());
				}
				{
					String dn = "cn=/123,ou=groups";
					CompareRequest req = new CompareRequest(dn, "member", String
							.format("cn=%s,ou=system", username1));
					CompareResult result = connInfo.connection.compare(req);
					assertFalse(result.compareMatched());
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		});

	}

	@Test
	public void testGroupofnames() throws Exception
	{
		// the goal here is to test
		// - bind + get DN + limited search
		//
		String extended_conf = "" + "ldap.servers.1=%s\n" + "ldap.ports.1=%d\n"
				+ "ldap.trustAllServerCertificates=true\n"
				+ "ldap.translationProfile=dummy\n" + "ldap.userDNSearchKey=s1\n"
				+ "ldap.bindAs=system\n"
				+ String.format("ldap.systemDN=cn=%s,ou=system\n", username1)
				+ String.format("ldap.systemPassword=%s\n", apass1)
				+ String.format("ldap.additionalSearch.s1.filter=(&(mail=%s)(objectClass=person))\n",
						email1)
				+ String.format("ldap.additionalSearch.s1.baseName=cn=%s,ou=system\n",
						username1)
				+ "ldap.additionalSearch.s1.selectedAttributes=dn,mail";
		LdapClientConfiguration ldapConfig = getLdapClientConfig(extended_conf, null, 0,
				null, null);

		// test LDAP connection via LdapClient
		LdapClient client = new LdapClient("test");
		client.bindAndExecute(username1, apass1, ldapConfig, (connInfo) -> {
			try
			{
				String[] queriedAttributes = new String[] { "*" };
				SearchScope searchScope = ldapConfig.getSearchScope();
				int timeLimit = ldapConfig.getSearchTimeLimit();
				int sizeLimit = ldapConfig.getAttributesLimit();
				DereferencePolicy derefPolicy = ldapConfig.getDereferencePolicy();
				String filter = String.format(
						"(&(member=cn=%s,ou=system)(objectClass=groupofnames))",
						username1);
				String dn_str = "ou=system";
				ReadOnlySearchRequest searchRequest = new SearchRequest(dn_str,
						searchScope, derefPolicy, sizeLimit, timeLimit,
						false, filter, queriedAttributes);
				SearchResult result = connInfo.connection.search(searchRequest);
				SearchResultEntry entry = result
						.getSearchEntry("cn=/,ou=happygroups");
				assertTrue(entry != null);
			} catch (Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		});
	}
}
