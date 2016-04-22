/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.ldap.client.LdapAuthenticationException;
import pl.edu.icm.unity.ldap.client.LdapClient;
import pl.edu.icm.unity.ldap.client.LdapClientConfiguration;
import pl.edu.icm.unity.ldap.client.LdapProperties;
import pl.edu.icm.unity.ldap.client.LdapUtils;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.server.authn.remote.RemoteGroupMembership;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.schema.Schema;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;
import eu.emi.security.authn.x509.X509Credential;
import eu.emi.security.authn.x509.helpers.BinaryCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.emi.security.authn.x509.impl.SocketFactoryCreator;
import eu.unicore.security.canl.IAuthnAndTrustConfiguration;
import static pl.edu.icm.unity.ldap.client.LdapProperties.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class LdapTests
{
	private static InMemoryDirectoryServer ds;
	private static String port;
	private static String hostname;
	private static String sslPort;
	private static String sslHostname;
	
	private static PKIManagement pkiManagement;
	private static X509CertChainValidatorExt regularValidator, emptyValidator;
	
	@BeforeClass
	public static void startEmbeddedServer() throws Exception
	{
		InMemoryDirectoryServerConfig config =
				new InMemoryDirectoryServerConfig("dc=unity-example,dc=com");

		List<InMemoryListenerConfig> listenerConfigs = new ArrayList<>();
		
		BinaryCertChainValidator acceptAll = new BinaryCertChainValidator(true);
		KeystoreCredential credential = new KeystoreCredential("src/test/resources/demoKeystore.p12", 
				"the!uvos".toCharArray(), "the!uvos".toCharArray(), "uvos", "PKCS12");
		SSLServerSocketFactory serverSocketFactory = SocketFactoryCreator.getServerSocketFactory(credential, 
				acceptAll);
		SSLSocketFactory clientSocketFactory = SocketFactoryCreator.getSocketFactory(null, acceptAll);
		System.out.println(Arrays.toString(serverSocketFactory.getSupportedCipherSuites()));
		System.out.println(Arrays.toString(clientSocketFactory.getSupportedCipherSuites()));
		
		InMemoryListenerConfig sslListener = new InMemoryListenerConfig("SSL", InetAddress.getByName("localhost"), 
				0, serverSocketFactory, clientSocketFactory, null);
		InMemoryListenerConfig plainWithTlsListener = new InMemoryListenerConfig("plain", 
				InetAddress.getByName("localhost"), 0, null, null, clientSocketFactory);
		listenerConfigs.add(plainWithTlsListener);
		listenerConfigs.add(sslListener);
		config.setListenerConfigs(listenerConfigs);
		
		Schema def = Schema.getDefaultStandardSchema();
		Schema mini = Schema.getSchema("src/test/resources/nis-cut.ldif");
		Schema merged = Schema.mergeSchemas(mini, def);
		config.setSchema(merged);
		
		ds = new InMemoryDirectoryServer(config);
		ds.importFromLDIF(true, "src/test/resources/test-data.ldif");
		ds.startListening();
		LDAPConnection conn = ds.getConnection("plain");
		hostname = conn.getConnectedAddress();
		port = conn.getConnectedPort()+"";
		LDAPConnection sslConn = ds.getConnection("SSL");
		sslHostname = sslConn.getConnectedAddress();
		sslPort = sslConn.getConnectedPort()+"";
		
		regularValidator = new KeystoreCertChainValidator("src/test/resources/demoTruststore.jks", 
				"unicore".toCharArray(), "JKS", -1);
		emptyValidator = new KeystoreCertChainValidator("src/test/resources/empty.jks", 
				"the!empty".toCharArray(), "JKS", -1);
		
		pkiManagement = new PKIManagement()
		{
			@Override
			public Set<String> getValidatorNames() throws EngineException
			{
				return Collections.singleton("main");
			}
			
			@Override
			public X509CertChainValidatorExt getValidator(String name) throws EngineException
			{
				if (name.equals("REGULAR"))
					return regularValidator;
				if (name.equals("EMPTY"))
					return emptyValidator;
				throw new WrongArgumentException("No such validator " + name);
			}
			
			@Override
			public Set<String> getCredentialNames() throws EngineException
			{
				return null;
			}
			@Override
			public X509Credential getCredential(String name) throws EngineException
			{
				return null;
			}

			@Override
			public IAuthnAndTrustConfiguration getMainAuthnAndTrust()
			{
				return null;
			}

			@Override
			public Set<String> getCertificateNames() throws EngineException
			{
				return null;
			}

			@Override
			public X509Certificate getCertificate(String name) throws EngineException
			{
				return null;
			}

			@Override
			public void updateCertificate(String name, X509Certificate updated)
					throws EngineException
			{
			}

			@Override
			public void removeCertificate(String name) throws EngineException
			{
			}

			@Override
			public void addCertificate(String name, X509Certificate updated)
					throws EngineException
			{
			}
		};
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
			assertEquals("cn=user1,ou=users,dc=unity-example,dc=com", 
					ret.getIdentities().values().iterator().next().getName());
		} catch (Exception e)
		{
			e.printStackTrace();
			fail("authn only failed");
		}
	}
	
	@Test
	public void testSSL() throws Exception
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
		
		p.setProperty(PREFIX+TLS_TRUST_ALL, "false");
		p.setProperty(PREFIX+TRUSTSTORE, "REGULAR");
		lp = new LdapProperties(p);
		clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		client = new LdapClient("test");
		client.bindAndSearch("user1", "user1", clientConfig);
		
	}
	
	/**
	 * Requires external server. For unknown reason the inmemory unboundID server doesn't handle startTLS correctly.
	 * @throws Exception
	 */
	//@Test
	public void testStartTls() throws Exception
	{
		Properties p = new Properties();

		p.setProperty(PREFIX+SERVERS+"1", "centos6-unity1");
		p.setProperty(PREFIX+PORTS+"1", "389");
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
	public void testSimpleAttributes() throws LDAPException, LdapAuthenticationException, 
		KeyManagementException, NoSuchAlgorithmException
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
		clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		client = new LdapClient("test");
		ret = client.bindAndSearch("user1", "user1", clientConfig);
		assertEquals(2, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User1 surname"));
		assertTrue(containsAttribute(ret.getAttributes(), "cn", "user1"));
		

		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_BASE, "ou=groups,dc=unity-example,dc=com");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_FILTER, "(memberUid={USERNAME})");
		p.setProperty(PREFIX+ADV_SEARCH_PFX+"1."+ADV_SEARCH_ATTRIBUTES, "dummy  gidNumber");
		
		lp = new LdapProperties(p);
		clientConfig = new LdapClientConfiguration(lp, pkiManagement);
		client = new LdapClient("test");
		ret = client.bindAndSearch("user1", "user1", clientConfig);
		assertEquals(3, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User1 surname"));
		assertTrue(containsAttribute(ret.getAttributes(), "cn", "user1"));
		assertTrue(containsAttribute(ret.getAttributes(), "gidNumber", "1"));
	}

	@Test
	public void testMemberOfGroups() throws Exception
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

		
		p.setProperty(PREFIX+MEMBER_OF_GROUP_ATTRIBUTE, "cn");
		lp = new LdapProperties(p);
		clientConfig = new LdapClientConfiguration(lp, pkiManagement);
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
	public void testExtraSearches() throws Exception
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
	public void testUserDNSearch() throws Exception
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

		assertEquals(5, ret.getAttributes().size());
		assertTrue(containsAttribute(ret.getAttributes(), "sn", "User2 Surname"));
	}

	
	@Test
	public void testBindAsSystem() throws Exception
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

