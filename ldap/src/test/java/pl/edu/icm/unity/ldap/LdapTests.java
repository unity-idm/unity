/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import java.util.Properties;

import org.junit.Test;

import com.unboundid.ldap.sdk.LDAPException;

import static pl.edu.icm.unity.ldap.LdapProperties.*;
import static org.junit.Assert.*;

public class LdapTests
{
	@Test
	public void test()
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", "centos6-unity1");
		p.setProperty(PREFIX+PORTS+"1", "389");
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+BIND_ONLY, "true");
		LdapProperties lp = new LdapProperties(p);
		
		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp);
		
		LdapClient client = new LdapClient("test");
		
		try
		{
			client.bindAndSearch("user1", "user1", clientConfig);
		} catch (LDAPException e)
		{
			e.printStackTrace();
			fail("authn only failed");
		}
	}
}
