/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.ws;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.AttributeAssertionParser;
import eu.unicore.samly2.attrprofile.ParsedAttribute;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.elements.SAMLAttribute;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.samly2.exceptions.SAMLValidationException;
import eu.unicore.security.wsutil.samlclient.AuthnResponseAssertions;
import eu.unicore.security.wsutil.samlclient.SAMLAttributeQueryClient;
import eu.unicore.security.wsutil.samlclient.SAMLAuthnClient;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.saml.idp.ws.SamlIdPSoapEndpointFactory;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttribute;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.CertificateVerificatorFactory;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

public class TestSoapEndpoint extends DBIntegrationTestBase
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
			"unity.saml.groupAttribute=groups\n" +
			"unity.saml.groupSelection=all\n" +
			"unity.saml.credential=MAIN\n";

	@Before
	public void setup()
	{
		try
		{
			setupMockAuthn();
			createUsers();
			AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
					10, 100, -1, 600);
			realmsMan.addRealm(realm);
			List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
			authnCfg.add(new AuthenticatorSet(Collections.singleton("Apass")));
			authnCfg.add(new AuthenticatorSet(Collections.singleton("Acert")));
			endpointMan.deploy(SamlIdPSoapEndpointFactory.NAME, "endpoint1", "/saml", "desc", 
					authnCfg, SAML_ENDP_CFG, realm.getName());
			List<EndpointDescription> endpoints = endpointMan.getEndpoints();
			assertEquals(1, endpoints.size());

			httpServer.start();
		} catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void testAuthn() throws Exception
	{
		String attrWSUrl = "https://localhost:2443/saml" + SamlIdPSoapEndpointFactory.SERVLET_PATH +
				"/AuthenticationService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("mockPassword1");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		
		NameID localIssuer = new NameID("unicore receiver", SAMLConstants.NFORMAT_ENTITY);
		SAMLAuthnClient client = new SAMLAuthnClient(attrWSUrl, clientCfg);
		try
		{
			client.authenticate(SAMLConstants.NFORMAT_DN, localIssuer, "http://somehost/consumer");
			fail("authenticated with wrong format");
		} catch (SAMLServerException e) {
			//expected
		}
		
		AuthnResponseAssertions resp = client.authenticate(SAMLConstants.NFORMAT_PERSISTENT, 
				localIssuer, "http://somehost/consumer");
		
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_PERSISTENT, 3);
		
		clientCfg.setHttpPassword("wrong");
		client = new SAMLAuthnClient(attrWSUrl, clientCfg);
		try
		{
			client.authenticate(SAMLConstants.NFORMAT_PERSISTENT, localIssuer, "http://somehost/consumer");
			fail("authenticated with wrong password");
		} catch (SAMLServerException e) {
			//expected
		}

		clientCfg.setHttpAuthn(false);
		client = new SAMLAuthnClient(attrWSUrl, clientCfg);
		try
		{
			client.authenticate(SAMLConstants.NFORMAT_PERSISTENT, localIssuer, "http://somehost/consumer");
			fail("authenticated without credential");
		} catch (SAMLServerException e) {
			//expected
		}
		
		//only TLS
		clientCfg.setSslAuthn(true);
		client = new SAMLAuthnClient(attrWSUrl, clientCfg);
		resp = client.authenticate(SAMLConstants.NFORMAT_DN, localIssuer, "http://somehost/consumer");
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_DN, 4);
		
		//both, both ok, the first configured, i.e. the password should be used.
		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpPassword("mockPassword1");
		client = new SAMLAuthnClient(attrWSUrl, clientCfg);
		resp = client.authenticate(localIssuer, "http://somehost/consumer");
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_PERSISTENT, 3);
		
		//both but password wrong so TLS should be used.
		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpPassword("wrong");
		client = new SAMLAuthnClient(attrWSUrl, clientCfg);
		resp = client.authenticate(localIssuer, "http://somehost/consumer");
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_PERSISTENT, 4);
	}

	
	private void checkAuthnResponse(AuthnResponseAssertions resp, String expectedFormat,
			int expectedAttrs) throws SAMLValidationException
	{
		assertEquals(1, resp.getAuthNAssertions().size());
		assertEquals(1, resp.getAttributeAssertions().size());
		assertEquals(0, resp.getOtherAssertions().size());
		assertNotNull(resp.getAuthNAssertions().get(0).getSubjectName());
		assertEquals(expectedFormat, resp.getAuthNAssertions().get(0).getSubjectNameFormat());
		assertEquals(expectedAttrs, resp.getAttributeAssertions().get(0).getAttributes().size());
	}
	
	@Test
	public void testAttributes() throws Exception
	{
		String attrWSUrl = "https://localhost:2443/saml" + SamlIdPSoapEndpointFactory.SERVLET_PATH +
				"/AssertionQueryService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("mockPassword1");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		NameID localIssuer = null;
		
		SAMLAttributeQueryClient client = new SAMLAttributeQueryClient(attrWSUrl, clientCfg);
		AttributeAssertionParser a = client.getAssertion(new NameID("CN=Test UVOS,O=UNICORE,C=EU", SAMLConstants.NFORMAT_DN),
				localIssuer);
		assertEquals(4, a.getAttributes().size());
		ParsedAttribute a1 = a.getAttribute("stringA");
		assertNotNull(a1);
		assertEquals(0, a1.getStringValues().size());
		ParsedAttribute a2 = a.getAttribute("intA");
		assertNotNull(a2);
		assertEquals(1, a2.getStringValues().size());
		assertEquals("1", a2.getStringValues().get(0));
		ParsedAttribute a3 = a.getAttribute("floatA");
		assertNotNull(a3);
		assertEquals(3, a3.getStringValues().size());
		assertEquals("123.1", a3.getStringValues().get(0));
		assertEquals("124.1", a3.getStringValues().get(1));
		assertEquals("14.2", a3.getStringValues().get(2));
		ParsedAttribute a4 = a.getAttribute("groups");
		assertNotNull(a4);
		assertEquals(1, a4.getStringValues().size());
		assertEquals("/", a4.getStringValues().get(0));
		
		a = client.getAssertion(new NameID("CN=Test UVOS,O=UNICORE,C=EU", SAMLConstants.NFORMAT_DN), 
				localIssuer, new SAMLAttribute("floatA", null));
		assertEquals(1, a.getAttributes().size());
		a3 = a.getAttribute("floatA");
		assertNotNull(a3);
		assertEquals(3, a3.getStringValues().size());
		assertEquals("123.1", a3.getStringValues().get(0));
		assertEquals("124.1", a3.getStringValues().get(1));
		assertEquals("14.2", a3.getStringValues().get(2));


		SAMLAttribute queried = new SAMLAttribute("floatA", null);
		queried.addStringAttributeValue("124.1");
		a = client.getAssertion(new NameID("CN=Test UVOS,O=UNICORE,C=EU", SAMLConstants.NFORMAT_DN), 
				localIssuer, queried);
		assertEquals(1, a.getAttributes().size());
		a3 = a.getAttribute("floatA");
		assertNotNull(a3);
		assertEquals(1, a3.getStringValues().size());
		assertEquals("124.1", a3.getStringValues().get(0));
	}
	

	protected DefaultClientConfiguration getClientCfg() throws KeyStoreException, IOException
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(new KeystoreCredential("src/test/resources/demoKeystore.p12", 
				"the!uvos".toCharArray(), "the!uvos".toCharArray(), "uvos", "PKCS12"));
		clientCfg.setValidator(new KeystoreCertChainValidator("src/test/resources/demoTruststore.jks", 
				"unicore".toCharArray(), "JKS", -1));
		clientCfg.setSslEnabled(true);
		clientCfg.getHttpClientProperties().setSocketTimeout(3600000);
		return clientCfg;
	}
	
	protected void createUsers() throws Exception
	{
		Identity added1 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user1", true), 
				"cr-pass", EntityState.valid, false);
		EntityParam e1 = new EntityParam(added1);
		idsMan.setEntityCredential(e1, "credential1", new PasswordToken("mockPassword1").toJson());
		
		Identity added2 = idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user2", true), 
				"cr-certpass", EntityState.valid, false);
		EntityParam e2 = new EntityParam(added2);
		idsMan.addIdentity(new IdentityParam(X500Identity.ID, "CN=Test UVOS,O=UNICORE,C=EU", true), 
				e2, false);
		idsMan.setEntityCredential(new EntityParam(added2), "credential1", new PasswordToken("mockPassword2").toJson());
		
		attrsMan.addAttributeType(new AttributeType("stringA", new StringAttributeSyntax()));
		attrsMan.addAttributeType(new AttributeType("intA", new IntegerAttributeSyntax()));
		AttributeType fAT = new AttributeType("floatA", new FloatingPointAttributeSyntax());
		fAT.setMaxElements(100);
		attrsMan.addAttributeType(fAT);
		
		attrsMan.setAttribute(e1, new StringAttribute("stringA", "/", AttributeVisibility.full, "value"), false);
		attrsMan.setAttribute(e1, new IntegerAttribute("intA", "/", AttributeVisibility.local, 123), false);
		List<Double> vals = new ArrayList<Double>();
		vals.add(123.1);
		vals.add(124.1);
		vals.add(14.2);
		attrsMan.setAttribute(e1, new FloatingPointAttribute("floatA", "/", AttributeVisibility.full, vals), false);

		attrsMan.setAttribute(e2, new StringAttribute("stringA", "/", AttributeVisibility.full), false);
		attrsMan.setAttribute(e2, new IntegerAttribute("intA", "/", AttributeVisibility.full, 1), false);
		attrsMan.setAttribute(e2, new FloatingPointAttribute("floatA", "/", AttributeVisibility.full, vals), false);
		
		attrsMan.setAttribute(e1, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE, 
				"/", AttributeVisibility.local, "Inspector"), false);
		attrsMan.setAttribute(e2, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE, 
				"/", AttributeVisibility.local, "Regular User"), false);
	}
	
	protected void setupMockAuthn() throws Exception
	{
		CredentialDefinition credDef = new CredentialDefinition(
				PasswordVerificatorFactory.NAME, "credential1", "");
		credDef.setJsonConfiguration("{\"minLength\": 4, " +
				"\"historySize\": 5," +
				"\"minClassesNum\": 1," +
				"\"denySequences\": true," +
				"\"maxAge\": 30758400}");
		authnMan.addCredentialDefinition(credDef);
		CredentialDefinition credDef2 = new CredentialDefinition(
				CertificateVerificatorFactory.NAME, "credential2", "");
		credDef2.setJsonConfiguration("");
		authnMan.addCredentialDefinition(credDef2);
		
		CredentialRequirements cr = new CredentialRequirements("cr-pass", "", 
				Collections.singleton(credDef.getName()));
		authnMan.addCredentialRequirement(cr);
		
		Set<String> creds = new HashSet<String>();
		Collections.addAll(creds, credDef.getName(), credDef2.getName());
		CredentialRequirements cr3 = new CredentialRequirements("cr-certpass", "", creds);
		authnMan.addCredentialRequirement(cr3);
		
		authnMan.createAuthenticator("Apass", "password with cxf-httpbasic", null, "", "credential1");
		authnMan.createAuthenticator("Acert", "certificate with cxf-certificate", null, "", "credential2");
	}

	
	
}
