/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.security.etd.TrustDelegation;
import eu.unicore.security.wsutil.samlclient.AuthnResponseAssertions;
import eu.unicore.security.wsutil.samlclient.SAMLAuthnClient;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.authz.RoleAttributeTypeProvider;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.cert.CertificateVerificator;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

/**
 * @author K. Benedyczak
 */
public class TestSoapETD extends DBIntegrationTestBase
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
	private AuthenticationFlowManagement authnFlowMan;
	
	@Before
	public void setup()
	{
		try
		{
			setupMockAuthn();
			createUsers();
			AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
					10, 100, RememberMePolicy.disallow , 1, 600);
			realmsMan.addRealm(realm);
			
			authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
					"flow1", Policy.NEVER,
					Sets.newHashSet("Acert")));
	
			EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), "desc",
					 Lists.newArrayList("flow1"), SAML_ENDP_CFG, realm.getName());
			endpointMan.deploy(SamlUnicoreSoapEndpoint.NAME, "endpoint1", "/saml", cfg);
			List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
			assertEquals(1, endpoints.size());

			httpServer.start();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void testETDAuthn() throws Exception
	{
		String attrWSUrl = "https://localhost:52443/saml" + SamlUnicoreSoapEndpoint.SERVLET_PATH +
				"/AuthenticationService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setSslAuthn(true);
		
		NameID localIssuer = new NameID("unicore@example.com", SAMLConstants.NFORMAT_EMAIL);
		SAMLAuthnClient client = new SAMLAuthnClient(attrWSUrl, clientCfg);
		try
		{
			client.authenticate(SAMLConstants.NFORMAT_DN, localIssuer, "http://somehost/consumer");
			fail("ETD authenticated with wrong local issuer format");
		} catch (SAMLResponderException e) {
			//expected
		}

		localIssuer = new NameID("CN=some server", SAMLConstants.NFORMAT_DN);
		client = new SAMLAuthnClient(attrWSUrl, clientCfg);
		try
		{
			client.authenticate(SAMLConstants.NFORMAT_UNSPEC, localIssuer, "http://somehost/consumer");
			fail("ETD authenticated with wrong requested identity format");
		} catch (SAMLResponderException e) {
			//expected
		}
		
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
	
	protected DefaultClientConfiguration getClientCfg() throws KeyStoreException, IOException
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.getHttpClientProperties().setSocketTimeout(3600000);
		return clientCfg;
	}
	
	protected void createUsers() throws Exception
	{
		Identity added2 = idsMan.addEntity(new IdentityParam(X500Identity.ID, DEMO_SERVER_DN), 
				"cr-cert", EntityState.valid, false);
		EntityParam e2 = new EntityParam(added2);
		eCredMan.setEntityCredential(e2, "credential2", "");
		
		attrsMan.createAttribute(e2, EnumAttribute.of(RoleAttributeTypeProvider.AUTHORIZATION_ROLE, 
				"/", "Regular User"));
	}
	
	protected void setupMockAuthn() throws Exception
	{
		CredentialDefinition credDef2 = new CredentialDefinition(
				CertificateVerificator.NAME, "credential2");
		credDef2.setConfiguration("");
		credMan.addCredentialDefinition(credDef2);
		
		Set<String> creds = new HashSet<String>();
		Collections.addAll(creds, credDef2.getName());
		CredentialRequirements cr3 = new CredentialRequirements("cr-cert", "", creds);
		credReqMan.addCredentialRequirement(cr3);
		
		authnMan.createAuthenticator("Acert", "certificate", "", "credential2");
	}
}
