/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import org.junit.BeforeClass;
import org.junit.Test;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.ldap.client.LdapAuthenticationException;
import pl.edu.icm.unity.ldap.client.LdapClient;
import pl.edu.icm.unity.ldap.client.LdapClientConfiguration;
import pl.edu.icm.unity.ldap.client.LdapProperties;
import pl.edu.icm.unity.ldap.endpoint.LdapEndpointFactory;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LdapEndpointTests extends DBIntegrationTestBase
{
	public static final String ldapEndpointHostname = "127.0.0.1";
	public static final int ldapEndpointPort = 389;
    public static final String ldapEndpointConfiguration =
		"unity.ldapServer.host=" + ldapEndpointHostname + "\n" +
		"unity.ldapServer.ldapPort=" + ldapEndpointPort + "\n" +
		"unity.ldapServer.ldapsPort=636\n" +
		"unity.ldapServer.groupQuery=ougroups\n" +
		"unity.ldapServer.userQuery=cn\n" +
		"unity.ldapServer.groupMember=member\n" +
		"unity.ldapServer.groupMemberUserRegexp=cn\n" +
		"unity.ldapServer.returnedUserAttributes=cn,entryDN,jpegPhoto\n";

    public static final String ldapClientConfiguration =
        "ldap.servers.1=" + ldapEndpointHostname + "\n" +
        "ldap.ports.1=" + ldapEndpointPort + "\n" +
        "ldap.userDNTemplate=cn={USERNAME},ou=dontcare,dc=alsodontcare\n" +
        "ldap.authenticateOnly=true\n" +
        "ldap.trustAllServerCertificates=true\n" +
        "ldap.translationProfile=dummy\n";

	@Test
	public void testBind() throws Exception
	{
		List<AuthenticationOptionDescription> authnCfg = new ArrayList<AuthenticationOptionDescription>();
		authnCfg.add(new AuthenticationOptionDescription("pwdLdapSimple"));
		endpointMan.deploy(LdapEndpointFactory.NAME,
			"endpoint1", new I18nString("endpoint1"),
			"/mock", "desc", authnCfg, ldapEndpointConfiguration, "defaultRealm");
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		// now test bind
		Properties p = new Properties();
        p.load(new StringReader(ldapClientConfiguration));
		LdapProperties lp = new LdapProperties(p);

		LdapClientConfiguration clientConfig = new LdapClientConfiguration(lp, null);

		LdapClient client = new LdapClient("test");

        // test binding that should SUCCEED
        client.bindAndSearch("admin", "a", clientConfig);

        // test binding that should FAIL
        try
        {
            client.bindAndSearch("user1", "wrong", clientConfig);
            fail("authenticated with a wrong password");
        } catch (LdapAuthenticationException e)
        {
            //ok, expected
        }

    }

}

