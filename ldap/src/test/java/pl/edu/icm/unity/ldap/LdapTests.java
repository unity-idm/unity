/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.Schema;

import static pl.edu.icm.unity.ldap.LdapProperties.*;
import static org.junit.Assert.*;

public class LdapTests
{
	private static InMemoryDirectoryServer ds;
	private static String port;
	private static String hostname;
	
	@BeforeClass
	public static void startEmbeddedServer() throws Exception
	{
		InMemoryDirectoryServerConfig config =
				new InMemoryDirectoryServerConfig("dc=unity-example,dc=com");

		Schema def = Schema.getDefaultStandardSchema();
		Schema mini = Schema.getSchema("src/test/resources/nis-cut.ldif");
		Schema merged = Schema.mergeSchemas(mini, def);
		config.setSchema(merged);
		
		ds = new InMemoryDirectoryServer(config);
		ds.importFromLDIF(true, "src/test/resources/test-data.ldif");
		ds.startListening();
		LDAPConnection conn = ds.getConnection();
		hostname = conn.getConnectedAddress();
		port = conn.getConnectedPort()+"";
	}
	
	@AfterClass
	public static void shutdown()
	{
		ds.shutDown(true);
	}
	
	@Test
	public void testBind()
	{
		Properties p = new Properties();

		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+BIND_ONLY, "true");
		LdapProperties lp = new LdapProperties(p);
		
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp);
		
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
		
		try
		{
			RemotelyAuthenticatedInput ret = client.bindAndSearch("user1", "user1", clientConfig);
			assertEquals("test", ret.getIdpName());
			assertEquals(0, ret.getAttributes().size());
			assertEquals(0, ret.getGroups().size());
			assertEquals(1, ret.getIdentities().size());
			assertEquals("cn=user1,ou=users,dc=unity-example,dc=com", ret.getIdentities().get(0).getName());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("authn only failed");
		}
	}
	
	@Test
	public void testSimpleAttributes() throws LDAPException, LdapAuthenticationException, 
		KeyManagementException, NoSuchAlgorithmException
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+VALID_USERS_FILTER, "(!(cn=user2))");

		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp);
		LdapClient client = new LdapClient("test");
		
		try
		{
			client.bindAndSearch("user2", "user1", clientConfig);
			fail("authenticated with a username which should be filtered out");
		} catch (LdapAuthenticationException e)
		{
			//ok, expected
		}
		
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user1", "user1", clientConfig);

		assertEquals(0, ret.getGroups().size());
		assertEquals(4, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User1 surname"));
		assertTrue(containsAttribute(ret.getAttributes(), "cn", "user1"));
		assertTrue(containsAttribute(ret.getAttributes(), "userPassword", "user1"));
		assertTrue(containsAttribute(ret.getAttributes(), "objectClass", "inetOrgPerson", 
				"organizationalPerson", "person", "top"));
		
		p.setProperty(PREFIX+ATTRIBUTES+"1", "sn");
		p.setProperty(PREFIX+ATTRIBUTES+"2", "cn");
		
		lp = new LdapProperties(p);
		clientConfig = new LdapClientConfiguration(lp);
		client = new LdapClient("test");
		ret = client.bindAndSearch("user1", "user1", clientConfig);
		assertEquals(2, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User1 surname"));
		assertTrue(containsAttribute(ret.getAttributes(), "cn", "user1"));
	}

	@Test
	public void testMemberOfGroups() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", hostname);
		p.setProperty(PREFIX+PORTS+"1", port);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+MEMBER_OF_ATTRIBUTE, "secretary");

		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user2", "user1", clientConfig);
		assertEquals(2, ret.getGroups().size());
		assertTrue(containsGroup(ret.getGroups(), "cn=nice,dc=org"));
		assertTrue(containsGroup(ret.getGroups(), "cn=nicer,dc=org"));

		
		p.setProperty(PREFIX+MEMBER_OF_GROUP_ATTRIBUTE, "cn");
		lp = new LdapProperties(p);
		clientConfig = new LdapClientConfiguration(lp);
		client = new LdapClient("test");
		ret = client.bindAndSearch("user2", "user1", clientConfig);
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
		
		LdapProperties lp = new LdapProperties(p);
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp);
		LdapClient client = new LdapClient("test");
		RemotelyAuthenticatedInput ret = client.bindAndSearch("user1", "user1", clientConfig);

		assertEquals(3, ret.getGroups().size());
		assertTrue(containsGroup(ret.getGroups(), "gr1"));
		assertTrue(containsGroup(ret.getGroups(), "g1"));
		assertTrue(containsGroup(ret.getGroups(), "g2"));
	}
	
	private boolean containsGroup(List<RemoteGroupMembership> groups, String group)
	{
		for (RemoteGroupMembership ra: groups)
		{
			if (ra.getName().equals(group))
				return true;
		}
		return false;
	}

	private boolean containsAttribute(List<RemoteAttribute> attrs, String attr, String... values)
	{
		for (RemoteAttribute ra: attrs)
		{
			if (ra.getName().equals(attr))
			{
				for (int i=0; i<values.length; i++)
					if (!ra.getValues().get(i).equals(values[i]))
						return false;
				return true;
			}
		}
		return false;
	}
}

