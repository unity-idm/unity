/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static pl.edu.icm.unity.ldap.client.LdapProperties.ADV_SEARCH_ATTRIBUTES;
import static pl.edu.icm.unity.ldap.client.LdapProperties.ADV_SEARCH_BASE;
import static pl.edu.icm.unity.ldap.client.LdapProperties.ADV_SEARCH_FILTER;
import static pl.edu.icm.unity.ldap.client.LdapProperties.ADV_SEARCH_PFX;
import static pl.edu.icm.unity.ldap.client.LdapProperties.ATTRIBUTES;
import static pl.edu.icm.unity.ldap.client.LdapProperties.BIND_AS;
import static pl.edu.icm.unity.ldap.client.LdapProperties.BIND_ONLY;
import static pl.edu.icm.unity.ldap.client.LdapProperties.CONNECTION_MODE;
import static pl.edu.icm.unity.ldap.client.LdapProperties.GROUPS_BASE_NAME;
import static pl.edu.icm.unity.ldap.client.LdapProperties.GROUP_DEFINITION_MATCHBY_MEMBER_ATTR;
import static pl.edu.icm.unity.ldap.client.LdapProperties.GROUP_DEFINITION_MEMBER_ATTR;
import static pl.edu.icm.unity.ldap.client.LdapProperties.GROUP_DEFINITION_NAME_ATTR;
import static pl.edu.icm.unity.ldap.client.LdapProperties.GROUP_DEFINITION_OC;
import static pl.edu.icm.unity.ldap.client.LdapProperties.GROUP_DEFINITION_PFX;
import static pl.edu.icm.unity.ldap.client.LdapProperties.MEMBER_OF_ATTRIBUTE;
import static pl.edu.icm.unity.ldap.client.LdapProperties.MEMBER_OF_GROUP_ATTRIBUTE;
import static pl.edu.icm.unity.ldap.client.LdapProperties.PORTS;
import static pl.edu.icm.unity.ldap.client.LdapProperties.PREFIX;
import static pl.edu.icm.unity.ldap.client.LdapProperties.SERVERS;
import static pl.edu.icm.unity.ldap.client.LdapProperties.SYSTEM_DN;
import static pl.edu.icm.unity.ldap.client.LdapProperties.SYSTEM_PASSWORD;
import static pl.edu.icm.unity.ldap.client.LdapProperties.TLS_TRUST_ALL;
import static pl.edu.icm.unity.ldap.client.LdapProperties.TRANSLATION_PROFILE;
import static pl.edu.icm.unity.ldap.client.LdapProperties.TRUSTSTORE;
import static pl.edu.icm.unity.ldap.client.LdapProperties.USER_DN_SEARCH_KEY;
import static pl.edu.icm.unity.ldap.client.LdapProperties.USER_DN_TEMPLATE;
import static pl.edu.icm.unity.ldap.client.LdapProperties.VALID_USERS_FILTER;

import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.sdk.LDAPException;

import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.ldap.client.LdapAuthenticationException;
import pl.edu.icm.unity.ldap.client.LdapClient;
import pl.edu.icm.unity.ldap.client.LdapClientConfiguration;
import pl.edu.icm.unity.ldap.client.LdapProperties;
import pl.edu.icm.unity.ldap.client.LdapUtils;
import pl.edu.icm.unity.ldap.client.LdapProperties.BindAs;

public class LdapTest
{
	private static InMemoryDirectoryServer ds;
	private static String port;
	private static String hostname;
	private static String sslPort;
	private static String sslHostname;
	
	private static PKIManagement pkiManagement;
	
	@BeforeClass
	public static void startEmbeddedServer() throws Exception
	{
		EmbeddedDirectoryServer embeddedDirectoryServer = new EmbeddedDirectoryServer();
		ds = embeddedDirectoryServer.startEmbeddedServer();
		hostname = embeddedDirectoryServer.getPlainConnection().getConnectedAddress();
		port = embeddedDirectoryServer.getPlainConnection().getConnectedPort()+"";
		sslHostname = embeddedDirectoryServer.getSSLConnection().getConnectedAddress();
		sslPort = embeddedDirectoryServer.getSSLConnection().getConnectedPort()+"";
		pkiManagement = embeddedDirectoryServer.getPKIManagement4Client();
	}
	
	@AfterClass
	public static void shutdown()
	{
		ds.shutDown(true);
	}

	
	@Test
	public void shouldNotBindOnlyAsUserWithWrongPassword()
	{
		Properties p = new Properties();

		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+BIND_ONLY, "true");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		LdapProperties lp = new LdapProperties(p);
		
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		
		LdapClient client = new LdapClient("test");

		try
		{
			client.bindAndSearch("user1", "wrong", clientConfig);
			fail("authenticated with a wrong password");
		} catch (LdapAuthenticationException e)
		{
			//ok, expected
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("authn only failed");
		} 
	}
	
	@Test
	public void shouldNotBindOnlyAsUserWithWrongUsername()
	{
		Properties p = new Properties();

		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+BIND_ONLY, "true");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		LdapProperties lp = new LdapProperties(p);
		
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		
		LdapClient client = new LdapClient("test");

		try
		{
			client.bindAndSearch("wrong", "wrong", clientConfig);
			fail("authenticated with a wrong username");
		} catch (LdapAuthenticationException e)
		{
			//ok, expected
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("authn only failed");
		}
	}
	
	@Test
	public void shouldBindOnlyAsUserWithCorrectCredentials()
	{
		Properties p = new Properties();

		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+BIND_ONLY, "true");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		LdapProperties lp = new LdapProperties(p);
		
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		
		LdapClient client = new LdapClient("test");

		try
		{
			RemotelyAuthenticatedInput ret = client.bindAndSearch("user1", "user1", clientConfig);
			assertEquals("test", ret.getIdpName());
			assertEquals(0, ret.getAttributes().size());
			assertEquals(0, ret.getGroups().size());
			assertEquals(1, ret.getIdentities().size());
			assertEquals("cn=user1,ou=users,dc=unity-example,dc=com", 
					ret.getIdentities().values().iterator().next().getName());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("authn only failed");
		}
	}	
	
	
	@Test
	public void shouldConnectToSSLServerWithTruststore() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", sslHostname);
		p.setProperty(PREFIX+PORTS+"1", sslPort);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+BIND_ONLY, "true");
		p.setProperty(PREFIX+CONNECTION_MODE, "ssl");
		p.setProperty(PREFIX+TLS_TRUST_ALL, "false");
		p.setProperty(PREFIX+TRUSTSTORE, "REGULAR");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");

		client.bindAndSearch("user1", "user1", clientConfig);
	}

	@Test
	public void shouldConnectToSSLServerWithTrustAllSetting() throws Exception
	{
		Properties p = new Properties();

		p.setProperty(PREFIX+SERVERS+"1", sslHostname);
		p.setProperty(PREFIX+PORTS+"1", sslPort);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+BIND_ONLY, "true");
		p.setProperty(PREFIX+CONNECTION_MODE, "ssl");
		p.setProperty(PREFIX+TLS_TRUST_ALL, "true");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		
		try
		{
			RemotelyAuthenticatedInput ret = client.bindAndSearch("user1", "user1", clientConfig);
			assertEquals("test", ret.getIdpName());
			assertEquals(0, ret.getAttributes().size());
			assertEquals(0, ret.getGroups().size());
			assertEquals(1, ret.getIdentities().size());
			assertEquals("cn=user1,ou=users,dc=unity-example,dc=com", ret.getIdentities()
					.values().iterator().next().getName());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("authn only failed");
		}
	}
	
	/**
	 * For unknown reason the inmemory unboundID server doesn't handle startTLS correctly.
	 */
	@Test
	@Ignore
	public void testStartTls() throws Exception
	{
		Properties p = new Properties();

		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+BIND_ONLY, "true");
		p.setProperty(PREFIX+CONNECTION_MODE, "startTLS");
		p.setProperty(PREFIX+TLS_TRUST_ALL, "false");
		p.setProperty(PREFIX+TRUSTSTORE, "REGULAR");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		client.bindAndSearch("user1", "user1", clientConfig);
		
		p.setProperty(PREFIX+TRUSTSTORE, "EMPTY");
		
		lp = new LdapProperties(p);
		clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		client = new LdapClient("test");
		try
		{
			client.bindAndSearch("user1", "user1", clientConfig);
			fail("Managed to establish a TLS connection to a server with not trusted cert");
		} catch (LDAPException e)
		{
			//OK
		}
	}
	
	@Test
	public void shouldNotbindAsUserNotMatchingValidUserFilter() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+VALID_USERS_FILTER, "(!(cn=user2))");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");

		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		
		try
		{
			client.bindAndSearch("user2", "user1", clientConfig);
			fail("authenticated with a username which should be filtered out");
		} catch (LdapAuthenticationException e)
		{
			//ok, expected
		}
	}

	@Test
	public void shouldReturnDirectAttributesWithoutFilter() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");

		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user1", "user1", clientConfig);

		assertEquals(0, ret.getGroups().size());
		assertEquals(4, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User1 surname"));
		assertTrue(containsAttribute(ret.getAttributes(), "cn", "user1"));
		assertTrue(containsAttribute(ret.getAttributes(), "userPassword", "user1"));
		assertTrue(containsAttribute(ret.getAttributes(), "objectClass", "inetOrgPerson", 
				"organizationalPerson", "person", "top"));
	}
	
	@Test
	public void shouldReturnDirectAttributesWithOptions() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		p.setProperty(PREFIX+ATTRIBUTES+"1", "l");
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user2", "user1", clientConfig);

		assertEquals(0, ret.getGroups().size());
		assertEquals(2, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "l", "locality"));
		assertTrue(containsAttribute(ret.getAttributes(), "l;x-foo-option", "foo locality"));
	}
	
	@Test
	public void shouldFilterAttributes() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+VALID_USERS_FILTER, "(!(cn=user2))");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		p.setProperty(PREFIX+ATTRIBUTES+"1", "sn");
		p.setProperty(PREFIX+ATTRIBUTES+"2", "cn");

		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user1", "user1", clientConfig);
		assertEquals(2, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User1 surname"));
		assertTrue(containsAttribute(ret.getAttributes(), "cn", "user1"));
	}
	
	@Test
	public void shouldReturnAttributeFromAdvancedSearch() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+VALID_USERS_FILTER, "(!(cn=user2))");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_BASE, "ou=groups,dc=unity-example,dc=com");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_FILTER, "(memberUid={USERNAME})");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_ATTRIBUTES, "dummy  gidNumber");

		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user1", "user1", clientConfig);
		assertTrue(containsAttribute(ret.getAttributes(), "gidNumber", "1"));
	}

	@Test
	public void shouldReturnAttributeWithOptionsFromAdvancedSearch() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+VALID_USERS_FILTER, "(!(cn=user2))");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_BASE, "ou=groups,dc=unity-example,dc=com");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_FILTER, "(memberUid={USERNAME})");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_ATTRIBUTES, "dummy  gidNumber;x-foo-option");

		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user1", "user1", clientConfig);
		assertTrue(containsAttribute(ret.getAttributes(), "gidNumber;x-foo-option", "99"));
	}
	
	@Test
	public void shouldExtractMemberOfGroups() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+MEMBER_OF_ATTRIBUTE, "secretary");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");

		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user2", "user1", clientConfig);
		assertEquals(2, ret.getGroups().size());
		assertTrue(containsGroup(ret.getGroups(), "cn=nice,dc=org"));
		assertTrue(containsGroup(ret.getGroups(), "cn=nicer,dc=org"));
	}

	@Test
	public void shouldExtractMemberOfGroupsConvertingToSimpleName() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+MEMBER_OF_ATTRIBUTE, "secretary");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		p.setProperty(PREFIX+MEMBER_OF_GROUP_ATTRIBUTE, "cn");

		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user2", "user1", clientConfig);
		assertEquals(2, ret.getGroups().size());
		assertTrue(containsGroup(ret.getGroups(), "nice"));
		assertTrue(containsGroup(ret.getGroups(), "nicer"));
	}

	@Test
	public void testGroupsScanning() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+GROUPS_BASE_NAME, "dc=unity-example,dc=com");
		p.setProperty(PREFIX+GROUP_DEFINITION_PFX+"1."+GROUP_DEFINITION_OC, "posixGroup");
		p.setProperty(PREFIX+GROUP_DEFINITION_PFX+"1."+GROUP_DEFINITION_MEMBER_ATTR, "memberUid");
		p.setProperty(PREFIX+GROUP_DEFINITION_PFX+"1."+GROUP_DEFINITION_NAME_ATTR, "cn");
		p.setProperty(PREFIX+GROUP_DEFINITION_PFX+"1."+GROUP_DEFINITION_MATCHBY_MEMBER_ATTR, "cn");
		p.setProperty(PREFIX+GROUP_DEFINITION_PFX+"2."+GROUP_DEFINITION_OC, "groupOfNames");
		p.setProperty(PREFIX+GROUP_DEFINITION_PFX+"2."+GROUP_DEFINITION_MEMBER_ATTR, "member");
		p.setProperty(PREFIX+GROUP_DEFINITION_PFX+"2."+GROUP_DEFINITION_NAME_ATTR, "cn");
		p.setProperty(PREFIX+GROUP_DEFINITION_PFX+"3."+GROUP_DEFINITION_OC, "groupOfUniqueNames");
		p.setProperty(PREFIX+GROUP_DEFINITION_PFX+"3."+GROUP_DEFINITION_MEMBER_ATTR, "uniqueMember");
		p.setProperty(PREFIX+GROUP_DEFINITION_PFX+"3."+GROUP_DEFINITION_NAME_ATTR, "cn");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user1", "user1", clientConfig);

		assertEquals(3, ret.getGroups().size());
		assertTrue(containsGroup(ret.getGroups(), "gr1"));
		assertTrue(containsGroup(ret.getGroups(), "g1"));
		assertTrue(containsGroup(ret.getGroups(), "g2"));
	}

	@Test
	public void shouldReturnAttributesFromExtraSearch() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+ATTRIBUTES+"1", "sn");

		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_BASE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_FILTER, "(sn={USERNAME})");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_ATTRIBUTES, "secretary");	
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user2", "user1", clientConfig);

		assertEquals(2, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "secretary", "cn=extra2,dc=org"));
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User2 Surname"));
	}

	@Test
	public void shouldReturnAttributesWithBindsAsUserAndDNSearchWithSystemCredentials() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_SEARCH_KEY, "1");
		p.setProperty(PREFIX+ATTRIBUTES+"1", "sn");
		p.setProperty(PREFIX+SYSTEM_DN, "cn=user1,ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+SYSTEM_PASSWORD, "user1");	

		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_BASE, "ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_FILTER, "(sn={USERNAME})");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.bindAndSearch("User2 Surname", "user1", clientConfig);

		assertEquals(1, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User2 Surname"));
	}

	
	@Test
	public void shouldSearchForAttributesWhenUsingBindsAsSystem() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+ATTRIBUTES+"1", "sn");

		p.setProperty(PREFIX+BIND_AS, "system");
		p.setProperty(PREFIX+SYSTEM_DN, "cn=user1,ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+SYSTEM_PASSWORD, "user1");	
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user2", "user1", clientConfig);

		assertEquals(1, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User2 Surname"));
	}
	
	
	@Test
	public void shouldReturnAttributesWithUserTemplateAndAnonymousConnect() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+ATTRIBUTES+"1", "sn");
		p.setProperty(PREFIX+BIND_AS, BindAs.none.toString());
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.search("user2", clientConfig);

		assertEquals(1, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User2 Surname"));
	}
	
	@Test
	public void shouldReturnAttributesWithUserSearchAndAnonymousConnect() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+ATTRIBUTES+"1", "ou");
		p.setProperty(PREFIX+BIND_AS, BindAs.none.toString());
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		p.setProperty(PREFIX+USER_DN_SEARCH_KEY, "1");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_BASE, "ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_FILTER, "(sn={USERNAME})");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "dummy");
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.search("user2", clientConfig);

		assertEquals(1, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "ou", "grant2"));
	}	
	
	
	private boolean containsGroup(Map<String, RemoteGroupMembership> groups, String group)
	{
		return groups.containsKey(group);
	}

	private boolean containsAttribute(Map<String, RemoteAttribute> attrs, String attr, String... values)
	{
		RemoteAttribute ra = attrs.get(attr);
		if (ra == null)
			return false;
		for (int i=0; i<values.length; i++)
			if (!ra.getValues().get(i).equals(values[i]))
				return false;
		return true;
	}
	
	@Test
	public void extractorReturnsId()
	{
		String extracted = LdapUtils.extractUsername("CN=myId,CN=b,O=foo", Pattern.compile("CN=([^,]+),CN=.*"));
		
		assertThat(extracted, is("myId"));
	}
}

