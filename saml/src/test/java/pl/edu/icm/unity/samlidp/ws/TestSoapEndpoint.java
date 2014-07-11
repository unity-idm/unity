/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.samlidp.ws;

import static org.junit.Assert.*;

import org.junit.Test;

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
import pl.edu.icm.unity.saml.idp.ws.SamlIdPSoapEndpointFactory;
import pl.edu.icm.unity.samlidp.AbstractTestIdpBase;

public class TestSoapEndpoint extends AbstractTestIdpBase
{
	/**
	 * Tests authentication and attribute query of dynamic identities.
	 */
	@Test
	public void testDynamicIdentityTypes() throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlIdPSoapEndpointFactory.SERVLET_PATH +
				"/AuthenticationService";
		String attrWSUrl = "https://localhost:52443/saml" + SamlIdPSoapEndpointFactory.SERVLET_PATH +
				"/AssertionQueryService";
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("mockPassword1");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		NameID localIssuer = new NameID("unicore receiver", SAMLConstants.NFORMAT_ENTITY);

		SAMLAuthnClient authnClient = new SAMLAuthnClient(authnWSUrl, clientCfg);
		SAMLAttributeQueryClient attrClient = new SAMLAttributeQueryClient(attrWSUrl, clientCfg);
		
		AuthnResponseAssertions resp = authnClient.authenticate(SAMLConstants.NFORMAT_PERSISTENT, 
				localIssuer, "http://somehost/consumer");
		
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_PERSISTENT, 5);
		String persistentTargetedId = resp.getAuthNAssertions().get(0).getSubjectName();
		System.out.println("Targeted persistent id: " + persistentTargetedId);

		//ask about attributes using the persistent identifier
		AttributeAssertionParser a = attrClient.getAssertion(
				new NameID(persistentTargetedId, SAMLConstants.NFORMAT_PERSISTENT),
				localIssuer);
		assertEquals(5, a.getAttributes().size());
	}
	
	@Test
	public void testAuthn() throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlIdPSoapEndpointFactory.SERVLET_PATH +
				"/AuthenticationService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("mockPassword1");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		
		NameID localIssuer = new NameID("unicore receiver", SAMLConstants.NFORMAT_ENTITY);
		SAMLAuthnClient client = new SAMLAuthnClient(authnWSUrl, clientCfg);
		try
		{
			client.authenticate(SAMLConstants.NFORMAT_DN, localIssuer, "http://somehost/consumer");
			fail("authenticated with wrong format");
		} catch (SAMLServerException e) {
			//expected
		}
		
		AuthnResponseAssertions resp = client.authenticate(SAMLConstants.NFORMAT_PERSISTENT, 
				localIssuer, "http://somehost/consumer");
		
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_PERSISTENT, 5);
		
		clientCfg.setHttpPassword("wrong");
		client = new SAMLAuthnClient(authnWSUrl, clientCfg);
		try
		{
			client.authenticate(SAMLConstants.NFORMAT_PERSISTENT, localIssuer, "http://somehost/consumer");
			fail("authenticated with wrong password");
		} catch (SAMLServerException e) {
			//expected
		}

		clientCfg.setHttpAuthn(false);
		client = new SAMLAuthnClient(authnWSUrl, clientCfg);
		try
		{
			client.authenticate(SAMLConstants.NFORMAT_PERSISTENT, localIssuer, "http://somehost/consumer");
			fail("authenticated without credential");
		} catch (SAMLServerException e) {
			//expected
		}
		
		//only TLS
		clientCfg.setSslAuthn(true);
		client = new SAMLAuthnClient(authnWSUrl, clientCfg);
		resp = client.authenticate(SAMLConstants.NFORMAT_DN, localIssuer, "http://somehost/consumer");
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_DN, 7);
		
		//both, both ok, the first configured, i.e. the password should be used.
		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpPassword("mockPassword1");
		client = new SAMLAuthnClient(authnWSUrl, clientCfg);
		resp = client.authenticate(localIssuer, "http://somehost/consumer");
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_PERSISTENT, 5);
		
		//both but password wrong so TLS should be used.
		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpPassword("wrong");
		client = new SAMLAuthnClient(authnWSUrl, clientCfg);
		resp = client.authenticate(localIssuer, "http://somehost/consumer");
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_PERSISTENT, 7);
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
		String attrWSUrl = "https://localhost:52443/saml" + SamlIdPSoapEndpointFactory.SERVLET_PATH +
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
		assertEquals(6, a.getAttributes().size()); //2 identities, 1 group, 3 plain attributes
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
		ParsedAttribute a4 = a.getAttribute("memberOf");
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
}
