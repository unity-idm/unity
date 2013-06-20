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


import org.apache.xmlbeans.XmlOptions;
import org.junit.Before;
import org.junit.Test;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.samlclient.AuthnResponseAssertions;
import eu.unicore.samlclient.SAMLAttributeQueryClient;
import eu.unicore.samlclient.SAMLAuthnClient;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.assertion.Assertion;
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLServerException;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttribute;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.credential.CertificateVerificatorFactory;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
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
			"unity.saml.credential.format=pkcs12\n" +
			"unity.saml.credential.path=src/test/resources/demoKeystore.p12\n" +
			"unity.saml.credential.keyAlias=uvos\n" +
			"unity.saml.credential.password=the!uvos\n";

	@Before
	public void setup()
	{
		try
		{
			setupMockAuthn();
			createUsers();

			List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
			authnCfg.add(new AuthenticatorSet(Collections.singleton("Apass")));
			authnCfg.add(new AuthenticatorSet(Collections.singleton("Acert")));
			endpointMan.deploy(SamlIdPSoapEndpointFactory.NAME, "endpoint1", "/saml", "desc", authnCfg, SAML_ENDP_CFG);
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
		
		assertEquals(1, resp.getAuthNAssertions().size());
		assertEquals(1, resp.getAttributeAssertions().size());
		assertEquals(0, resp.getOtherAssertions().size());
		assertNotNull(resp.getAuthNAssertions().get(0).getSubjectName());
		assertEquals(SAMLConstants.NFORMAT_PERSISTENT, resp.getAuthNAssertions().get(0).getSubjectNameFormat());
		assertEquals(3, resp.getAttributeAssertions().get(0).getAttributes().size());
		System.out.println(resp.getAuthNAssertions().get(0).getXMLBeanDoc().xmlText(
				new XmlOptions().setSavePrettyPrint()));

		System.out.println("\n\n\n" +resp.getAttributeAssertions().get(0).getXMLBeanDoc().xmlText(
				new XmlOptions().setSavePrettyPrint()));
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
		
		SAMLAttributeQueryClient client = new SAMLAttributeQueryClient(attrWSUrl, clientCfg);
		Assertion a = client.getAssertion(new NameID("CN=Test UVOS,O=UNICORE,C=EU", SAMLConstants.NFORMAT_DN),
				new NameID("unicore receiver", SAMLConstants.NFORMAT_ENTITY));
		//TODO
		System.out.println(a.getXMLBeanDoc().xmlText(new XmlOptions().setSavePrettyPrint()));
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
		Identity added1 = idsMan.addIdentity(new IdentityParam(UsernameIdentity.ID, "user1", true, true), 
				"cr-pass", LocalAuthenticationState.outdated, false);
		EntityParam e1 = new EntityParam(added1);
		idsMan.setEntityCredential(e1, "credential1", "mockPassword1");
		
		Identity added2 = idsMan.addIdentity(new IdentityParam(UsernameIdentity.ID, "user2", true, true), 
				"cr-certpass", LocalAuthenticationState.outdated, false);
		EntityParam e2 = new EntityParam(added2);
		idsMan.addIdentity(new IdentityParam(X500Identity.ID, "CN=Test UVOS,O=UNICORE,C=EU", true, true), 
				e2, false);
		idsMan.setEntityCredential(new EntityParam(added2), "credential1", "mockPassword2");
		
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
		attrsMan.setAttribute(e1, new FloatingPointAttribute("floatA", "/", AttributeVisibility.full, vals), false);

		attrsMan.setAttribute(e2, new StringAttribute("stringA", "/", AttributeVisibility.full), false);
		attrsMan.setAttribute(e2, new IntegerAttribute("intA", "/", AttributeVisibility.full, 1), false);
		attrsMan.setAttribute(e2, new FloatingPointAttribute("floatA", "/", AttributeVisibility.full, 2.2), false);
		
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
