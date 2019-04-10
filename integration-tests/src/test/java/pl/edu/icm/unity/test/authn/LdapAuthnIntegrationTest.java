/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.authn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static pl.edu.icm.unity.ldap.client.LdapProperties.BIND_ONLY;
import static pl.edu.icm.unity.ldap.client.LdapProperties.PORTS;
import static pl.edu.icm.unity.ldap.client.LdapProperties.PREFIX;
import static pl.edu.icm.unity.ldap.client.LdapProperties.SERVERS;
import static pl.edu.icm.unity.ldap.client.LdapProperties.TRANSLATION_PROFILE;
import static pl.edu.icm.unity.ldap.client.LdapProperties.USER_DN_TEMPLATE;

import java.io.IOException;
import java.io.StringWriter;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.security.etd.TrustDelegation;
import eu.unicore.security.wsutil.samlclient.AuthnResponseAssertions;
import eu.unicore.security.wsutil.samlclient.SAMLAuthnClient;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.TranslationProfileManagement;
import pl.edu.icm.unity.engine.api.translation.in.IdentityEffectMode;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.in.action.MapIdentityActionFactory;
import pl.edu.icm.unity.ldap.EmbeddedDirectoryServer;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;
import pl.edu.icm.unity.unicore.samlidp.ws.SamlUnicoreSoapEndpoint;

@TestPropertySource(properties = { "unityConfig: src/test/resources/authn-tests/unityServer.conf" })
public class LdapAuthnIntegrationTest extends DBIntegrationTestBase
{
	private static final String SAML_ENDP_CFG = "unity.endpoint.sessionTimeout=3600\n" +
			"unity.saml.issuerURI=http://example-saml-idp.org\n" +
			"unity.saml.signResponses=asRequest\n" +
			"unity.saml.validityPeriod=3600\n" +
			"unity.saml.requestValidityPeriod=600\n" +
			"unity.saml.authenticationTimeout=20\n" +
			"unity.saml.spAcceptPolicy=all\n" +
			"#unity.saml.acceptedUriSP.xx=\n" +
			"#unity.saml.acceptedDNSP.xx=\n" +
			"unity.saml.defaultGroup=/\n" +
			"unity.saml.credential=MAIN\n";

	@Autowired
	private AuthenticatorManagement authnMan;
	@Autowired
	private TranslationProfileManagement tprofMan;
	
	private InMemoryDirectoryServer ds;
	private String ldapPort;
	private String ldapHostname;
	
	@Before
	public void setup()
	{
		try
		{
			startEmbeddedLdapServer();
			createTranslationProfile();
			setupLdapAuthenticator();
			createUser();
			AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
					10, 100, RememberMePolicy.disallow , 1, 600);
			realmsMan.addRealm(realm);
			
			EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), "desc",
					 Lists.newArrayList("ldap-password"), SAML_ENDP_CFG, realm.getName());
			endpointMan.deploy(SamlUnicoreSoapEndpoint.NAME, "endpoint1", "/saml", cfg);
			List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
			assertEquals(1, endpoints.size());

			httpServer.start();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@After
	public void shutdown()
	{
		ds.shutDown(true);
	}
	
	@Test
	public void testETDAuthn() throws Exception
	{
		String attrWSUrl = "https://localhost:52443/saml" + SamlUnicoreSoapEndpoint.SERVLET_PATH +
				"/AuthenticationService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		
		NameID localIssuer = new NameID("CN=some server", SAMLConstants.NFORMAT_DN);
		SAMLAuthnClient client = new SAMLAuthnClient(attrWSUrl, clientCfg);
		
		AuthnResponseAssertions resp = client.authenticate(SAMLConstants.NFORMAT_DN, 
				localIssuer, "http://somehost/consumer");
		
		assertEquals(1, resp.getAuthNAssertions().size());
		assertEquals(2, resp.getAttributeAssertions().size());
		assertEquals(0, resp.getOtherAssertions().size());
		assertNotNull(resp.getAuthNAssertions().get(0).getSubjectName());
		assertEquals(SAMLConstants.NFORMAT_DN, resp.getAuthNAssertions().get(0).getSubjectNameFormat());
		TrustDelegation delegation = new TrustDelegation(resp.getAttributeAssertions().get(1).getXMLBeanDoc());
		assertEquals(DEMO_SERVER_DN, delegation.getCustodianDN());
		assertEquals("http://example-saml-idp.org", delegation.getIssuerName());
		assertEquals("CN=some server", delegation.getSubjectName());
	}
	
	private static KeystoreCertChainValidator getClientValidator() throws KeyStoreException, IOException
	{
		return new KeystoreCertChainValidator("src/test/resources/authn-tests/demoTruststore.jks", 
				DEMO_KS_PASS.toCharArray(), "JKS", -1);
	}
	
	private DefaultClientConfiguration getClientCfg() throws KeyStoreException, IOException
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setValidator(getClientValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("user1");
		clientCfg.setHttpAuthn(true);
		clientCfg.getHttpClientProperties().setSocketTimeout(3600000);
		return clientCfg;
	}
	
	private void createUser() throws Exception
	{
		Identity added1 = idsMan.addEntity(new IdentityParam(X500Identity.ID, DEMO_SERVER_DN), 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, EntityState.valid, false);
		Attribute sa = EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, 
				"/", Lists.newArrayList(AuthorizationManagerImpl.USER_ROLE));
		attrsMan.createAttribute(new EntityParam(added1), sa);
	}
	
	private void setupLdapAuthenticator() throws Exception
	{
		Properties p = new Properties();
		p.setProperty(PREFIX+SERVERS+"1", ldapHostname);
		p.setProperty(PREFIX+PORTS+"1", ldapPort);
		p.setProperty(PREFIX+USER_DN_TEMPLATE, "cn={USERNAME},ou=users,dc=unity-example,dc=com");
		p.setProperty(PREFIX+BIND_ONLY, "true");
		p.setProperty(PREFIX+TRANSLATION_PROFILE, "ldapProfile");
		StringWriter writer = new StringWriter();
		p.store(writer, "");
		authnMan.createAuthenticator("ldap-password", "ldap", writer.toString(), null);
	}
	
	private void startEmbeddedLdapServer() throws Exception
	{
		KeystoreCredential keystoreCredential = new KeystoreCredential("src/test/resources/authn-tests/demoKeystore.p12", 
				DEMO_KS_PASS.toCharArray(), DEMO_KS_PASS.toCharArray(), DEMO_KS_ALIAS, "PKCS12");
		EmbeddedDirectoryServer embeddedDirectoryServer = new EmbeddedDirectoryServer(keystoreCredential,
				"src/test/resources/authn-tests");
		ds = embeddedDirectoryServer.startEmbeddedServer();
		ldapHostname = embeddedDirectoryServer.getPlainConnection().getConnectedAddress();
		ldapPort = embeddedDirectoryServer.getPlainConnection().getConnectedPort()+"";
	}
	
	private void createTranslationProfile() throws Exception
	{
		List<TranslationRule> rules = new ArrayList<>();
		TranslationAction action1 = new TranslationAction( 
				MapIdentityActionFactory.NAME, new String[] {
						X500Identity.ID, 
				"'" + DEMO_SERVER_DN + "'", 
				EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT, 
				IdentityEffectMode.CREATE_OR_MATCH.toString()});
		rules.add(new TranslationRule("true", action1));
		TranslationProfile toAdd = new TranslationProfile("ldapProfile", "", ProfileType.INPUT, rules);
		tprofMan.addProfile(toAdd);
	}

}
