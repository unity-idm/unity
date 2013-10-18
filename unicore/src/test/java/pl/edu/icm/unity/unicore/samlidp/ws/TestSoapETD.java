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
import eu.unicore.samly2.elements.NameID;
import eu.unicore.samly2.exceptions.SAMLResponderException;
import eu.unicore.security.etd.TrustDelegation;
import eu.unicore.security.wsutil.samlclient.AuthnResponseAssertions;
import eu.unicore.security.wsutil.samlclient.SAMLAuthnClient;
import eu.unicore.util.httpclient.DefaultClientConfiguration;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.stdext.attr.EnumAttribute;
import pl.edu.icm.unity.stdext.credential.CertificateVerificatorFactory;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.sysattrs.SystemAttributeTypes;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

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
			authnCfg.add(new AuthenticatorSet(Collections.singleton("Acert")));
			endpointMan.deploy(SamlUnicoreIdPSoapEndpointFactory.NAME, "endpoint1", "/saml", "desc", authnCfg, SAML_ENDP_CFG);
			List<EndpointDescription> endpoints = endpointMan.getEndpoints();
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
		String attrWSUrl = "https://localhost:2443/saml" + SamlUnicoreIdPSoapEndpointFactory.SERVLET_PATH +
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
		assertEquals("CN=Test UVOS,O=UNICORE,C=EU", delegation.getCustodianDN());
		assertEquals("http://example-saml-idp.org", delegation.getIssuerName());
		assertEquals("CN=some server", delegation.getSubjectName());
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
		Identity added2 = idsMan.addEntity(new IdentityParam(X500Identity.ID, "CN=Test UVOS,O=UNICORE,C=EU", true), 
				"cr-cert", EntityState.valid, false);
		EntityParam e2 = new EntityParam(added2);
		idsMan.setEntityCredential(e2, "credential2", "");
		
		attrsMan.setAttribute(e2, new EnumAttribute(SystemAttributeTypes.AUTHORIZATION_ROLE, 
				"/", AttributeVisibility.local, "Regular User"), false);
	}
	
	protected void setupMockAuthn() throws Exception
	{
		CredentialDefinition credDef2 = new CredentialDefinition(
				CertificateVerificatorFactory.NAME, "credential2", "");
		credDef2.setJsonConfiguration("");
		authnMan.addCredentialDefinition(credDef2);
		
		Set<String> creds = new HashSet<String>();
		Collections.addAll(creds, credDef2.getName());
		CredentialRequirements cr3 = new CredentialRequirements("cr-cert", "", creds);
		authnMan.addCredentialRequirement(cr3);
		
		authnMan.createAuthenticator("Acert", "certificate with cxf-certificate", null, "", "credential2");
	}
}
