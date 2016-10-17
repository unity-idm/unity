/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap;

import com.unboundid.ldap.sdk.*;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.mock.MockPasswordVerificatorFactory;
import pl.edu.icm.unity.ldap.client.*;
import pl.edu.icm.unity.ldap.endpoint.LdapEndpointFactory;
import pl.edu.icm.unity.server.authn.remote.RemoteIdentity;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.*;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static org.junit.Assert.*;

public class LdapEndpointTests extends DBIntegrationTestBase
{
	public static final String ldapEndpointHostname = "127.0.0.1";
    public static final int ldapEndpointPort = 389;
    public static final String ldapEndpointDNTemplate= "cn={USERNAME},ou=system";


    public static final String ldapEndpointConfiguration =
		"unity.ldapServer.host=" + ldapEndpointHostname + "\n" +
		"unity.ldapServer.ldapPort=" + ldapEndpointPort + "\n" +
		"unity.ldapServer.ldapsPort=636\n" +
		"unity.ldapServer.groupQuery=ougroups\n" +
		"unity.ldapServer.userQuery=cn\n" +
		"unity.ldapServer.groupMember=member\n" +
		"unity.ldapServer.groupMemberUserRegexp=cn\n" +
		"unity.ldapServer.returnedUserAttributes=cn,entryDN,jpegPhoto\n" +
        "unity.ldapServer.userNameAliases=cn,mail\n" +
        "unity.ldapServer.tls=true\n" +
        "unity.ldapServer.certPassword=test.p4ss\n" +
        "unity.ldapServer.keystoreName=ldap.test.keystore\n"
        ;

    private static final String ldapClientConfiguration =
        "ldap.servers.1=%s\n" +
        "ldap.ports.1=%d\n" +
        "ldap.userDNTemplate=%s\n" +
        "ldap.trustAllServerCertificates=true\n" +
        "ldap.translationProfile=dummy\n";

    public static LdapClientConfiguration getLdapClientConfig() throws IOException {
        return getLdapClientConfig(null, null, 0, null, null);
    }

    public static LdapClientConfiguration getLdapClientConfig(String appendix) throws IOException {
        return getLdapClientConfig(null, null, 0, null, appendix);
    }

    public static LdapClientConfiguration getLdapClientConfig(
        String baseconf, String hostname, int port, String dnTemplate, String appendix
    ) throws IOException
    {
        if (null == baseconf) {
            baseconf = ldapClientConfiguration;
        }
        if (null == hostname) {
            hostname = ldapEndpointHostname;
        }
        if (0 == port) {
            port = ldapEndpointPort;
        }
        if (null == dnTemplate) {
            dnTemplate = ldapEndpointDNTemplate;
        }
        if (null != appendix) {
            baseconf += appendix;
        }
        baseconf = String.format(baseconf, hostname, port, dnTemplate);

        Properties p = new Properties();
        p.load(new StringReader(baseconf));
        LdapProperties lp = new LdapProperties(p);
        return new LdapClientConfiguration(lp, null);
    }

	@Test
	public void testBind() throws Exception
	{
		List<AuthenticationOptionDescription> authnCfg = new ArrayList<AuthenticationOptionDescription>();
		authnCfg.add(new AuthenticationOptionDescription("pwdLdapSimple"));
        EndpointConfiguration cfg = new EndpointConfiguration(
            new I18nString("endpoint1"), "desc", authnCfg, ldapEndpointConfiguration, "defaultRealm"
        );
		endpointMan.deploy(LdapEndpointFactory.NAME, "endpoint1", "/mock", cfg);
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

        LdapClient client = new LdapClient("test");

        // test binding that should SUCCEED
        for (String s : new String[] {
            "ldap.connectionMode=plain\nldap.authenticateOnly=true",
            "ldap.connectionMode=startTLS\nldap.authenticateOnly=true",
        })
        {
            client.bindAndSearch("admin", "a", getLdapClientConfig(s));
        }

        // test binding that should FAIL
        try
        {
            client.bindAndSearch("user1", "wrong", getLdapClientConfig());
            fail("authenticated with a wrong password");
        } catch (LdapAuthenticationException e)
        {
            //ok, expected
        }

    }

    @Test
    public void testLdapApi() throws Exception
    {
        // do the magic initialisation
        //
        setupPasswordAuthn();
        String credentialId = "credential1";
        String ldap_typeId = "password with ldap-simple";

        final String AUTHENTICATOR_PASS = "ApassLDAPT";
        authnMan.createAuthenticator(
            AUTHENTICATOR_PASS, ldap_typeId, null, "", credentialId
        );
        List<AuthenticationOptionDescription> authnCfg = new ArrayList<AuthenticationOptionDescription>();
        authnCfg.add(new AuthenticationOptionDescription(AUTHENTICATOR_PASS));

        // create a simple test user
        //
        String username = "clarin";
        String apass = "heslo";
        String email = "clarin@email.com";
        String role = AuthorizationManagerImpl.USER_ROLE;

        Identity user_id = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, username),
            CRED_REQ_PASS, EntityState.valid, false
        );
        idsMan.setEntityCredential(new EntityParam(user_id), credentialId,
            new PasswordToken(apass).toJson()
        );
        EnumAttribute sa = new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE,
            "/", AttributeVisibility.local, role);
        attrsMan.setAttribute(new EntityParam(user_id), sa, false);

        Identity email_id = idsMan.addEntity(new IdentityParam(EmailIdentity.ID, email),
            CRED_REQ_PASS, EntityState.valid, false
        );

        // merge identities email + username
        idsMan.mergeEntities(new EntityParam(user_id), new EntityParam(email_id), true);

        // deploy
        //
        EndpointConfiguration cfg = new EndpointConfiguration(
            new I18nString("ldapEndpoint"), "desc", authnCfg, ldapEndpointConfiguration, "defaultRealm"
        );
        endpointMan.deploy(LdapEndpointFactory.NAME, "ldapEndpoint", "/mock", cfg);
            List<EndpointDescription> endpoints = endpointMan.getEndpoints();
            assertEquals(1, endpoints.size());

        // the goal here is to test
        // - bind + get DN + limited search
        //
        String extended_conf = "" +
            "ldap.servers.1=%s\n" +
            "ldap.ports.1=%d\n" +
            "ldap.trustAllServerCertificates=true\n" +
            "ldap.translationProfile=dummy\n" +
            "ldap.userDNSearchKey=s1\n" +
            "ldap.bindAs=system\n" +
            String.format("ldap.systemDN=cn=%s,ou=system\n", username) +
            String.format("ldap.systemPassword=%s\n", apass) +
            String.format("ldap.additionalSearch.s1.filter=(&(mail=%s)(objectClass=person))\n", email) +
            String.format("ldap.additionalSearch.s1.baseName=cn=%s,ou=system\n", username) +
            "ldap.additionalSearch.s1.selectedAttributes=dn,mail";
        LdapClientConfiguration ldapConfig = getLdapClientConfig(extended_conf, null, 0, null, null);

        // test LDAP connection via LdapClient
        LdapClient client = new LdapClient("test");
        client.bindAndExecute(username, apass, ldapConfig, (connection, dn) -> {
            try {
                String[] queriedAttributes = ldapConfig.getQueriedAttributes();
                SearchScope searchScope = ldapConfig.getSearchScope();

                int timeLimit = ldapConfig.getSearchTimeLimit();
                int sizeLimit = ldapConfig.getAttributesLimit();
                DereferencePolicy derefPolicy = ldapConfig.getDereferencePolicy();
                Filter validUsersFilter = ldapConfig.getValidUsersFilter();
                // bind and search uses unsupported operations - search over arbitrary user attributes
                // therefore, hardcode dn to something Unity's LDAP server supports in general
                dn = "ou=system";
                ReadOnlySearchRequest searchRequest = new SearchRequest(dn, searchScope, derefPolicy,
                    sizeLimit, timeLimit, false, validUsersFilter, queriedAttributes);
                SearchResult result = connection.search(searchRequest);
                SearchResultEntry entry = result.getSearchEntry(dn);
                assertNotNull(entry);
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            return null;
        });
    }

}
