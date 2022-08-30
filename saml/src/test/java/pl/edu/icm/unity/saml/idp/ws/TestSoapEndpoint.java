/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.ws;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

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
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.saml.idp.AbstractTestIdpBase;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences;
import pl.edu.icm.unity.saml.idp.preferences.SamlPreferences.SPSettings;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttribute;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

public class TestSoapEndpoint extends AbstractTestIdpBase
{
	/**
	 * Tests authentication and attribute query of dynamic identities.
	 */
	@Test
	public void testDynamicIdentityTypes() throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
				"/AuthenticationService";
		String attrWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
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
		
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_PERSISTENT, 7);
		String persistentTargetedId = resp.getAuthNAssertions().get(0).getSubjectName();
		System.out.println("Targeted persistent id: " + persistentTargetedId);

		//ask about attributes using the persistent identifier
		AttributeAssertionParser a = attrClient.getAssertion(
				new NameID(persistentTargetedId, SAMLConstants.NFORMAT_PERSISTENT),
				localIssuer);
		assertEquals(7, a.getAttributes().size());
	}
	
	@Test
	public void shouldAuthenticateWithPasswordOnly() throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
				"/AuthenticationService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("mockPassword1");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		
		NameID localIssuer = new NameID("unicore receiver", SAMLConstants.NFORMAT_ENTITY);
		SAMLAuthnClient client = new SAMLAuthnClient(authnWSUrl, clientCfg);

		AuthnResponseAssertions resp = client.authenticate(SAMLConstants.NFORMAT_PERSISTENT, 
				localIssuer, "http://somehost/consumer");
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_PERSISTENT, 7);
	}

	@Test
	public void shouldNotAuthenticateWithoutCredential() throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
				"/AuthenticationService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(false);
		
		NameID localIssuer = new NameID("unicore receiver", SAMLConstants.NFORMAT_ENTITY);
		SAMLAuthnClient client = new SAMLAuthnClient(authnWSUrl, clientCfg);

		try
		{
			client.authenticate(SAMLConstants.NFORMAT_PERSISTENT, localIssuer, "http://somehost/consumer");
			fail("authenticated without credential");
		} catch (SAMLServerException e) {
			//expected
		}
	}
	
	@Test
	public void shouldNotAuthnWithIncorrectPasswordOnly() throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
				"/AuthenticationService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("wrong");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		
		NameID localIssuer = new NameID("unicore receiver", SAMLConstants.NFORMAT_ENTITY);
		SAMLAuthnClient client = new SAMLAuthnClient(authnWSUrl, clientCfg);
		try
		{
			client.authenticate(SAMLConstants.NFORMAT_PERSISTENT, localIssuer, "http://somehost/consumer");
			fail("authenticated with wrong password");
		} catch (SAMLServerException e) {
			//expected
		}
	}
	
	@Test
	public void shouldNotAuthenticateWithWrongFormat() throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
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
	}
	
	@Test
	public void shouldAuthnWithPasswordWhenTlsAlsoSet() throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
				"/AuthenticationService";
		DefaultClientConfiguration clientCfg = getClientCfg();
		NameID localIssuer = new NameID("unicore receiver", SAMLConstants.NFORMAT_ENTITY);

		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("mockPassword1");
		SAMLAuthnClient client = new SAMLAuthnClient(authnWSUrl, clientCfg);
		AuthnResponseAssertions resp = client.authenticate(localIssuer, "http://somehost/consumer");
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_PERSISTENT, 7);
	}
	
	@Test
	public void shouldAuthnWithTLSOnly() throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
				"/AuthenticationService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		NameID localIssuer = new NameID("unicore receiver", SAMLConstants.NFORMAT_ENTITY);
		clientCfg.setHttpAuthn(false);
		clientCfg.setSslAuthn(true);
		SAMLAuthnClient client = new SAMLAuthnClient(authnWSUrl, clientCfg);
		AuthnResponseAssertions resp = client.authenticate(SAMLConstants.NFORMAT_DN, localIssuer, "http://somehost/consumer");
		checkAuthnResponse(resp, SAMLConstants.NFORMAT_DN, 7);
	}

	@Test
	public void shouldAuthnWithTLSWhenPasswordIncorrect() throws Exception
	{
		String authnWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
				"/AuthenticationService";
		DefaultClientConfiguration clientCfg = getClientCfg();
		NameID localIssuer = new NameID("unicore receiver", SAMLConstants.NFORMAT_ENTITY);
		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("wrong");
		SAMLAuthnClient client = new SAMLAuthnClient(authnWSUrl, clientCfg);
		AuthnResponseAssertions resp = client.authenticate(localIssuer, "http://somehost/consumer");
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
		String attrWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
				"/AssertionQueryService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("mockPassword1");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		NameID localIssuer = null;
		
		SAMLAttributeQueryClient client = new SAMLAttributeQueryClient(attrWSUrl, clientCfg);
		AttributeAssertionParser a = client.getAssertion(new NameID(DEMO_SERVER_DN, 
				SAMLConstants.NFORMAT_DN), localIssuer);
		assertEquals(7, a.getAttributes().size()); //3 identities, 1 group, 3 plain attributes
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
		
		a = client.getAssertion(new NameID(DEMO_SERVER_DN, SAMLConstants.NFORMAT_DN), 
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
		a = client.getAssertion(new NameID(DEMO_SERVER_DN, SAMLConstants.NFORMAT_DN), 
				localIssuer, queried);
		assertEquals(1, a.getAttributes().size());
		a3 = a.getAttribute("floatA");
		assertNotNull(a3);
		assertEquals(1, a3.getStringValues().size());
		assertEquals("124.1", a3.getStringValues().get(0));
	}
	
	
	@Test
	public void testPreferences() throws Exception
	{
		String attrWSUrl = "https://localhost:52443/saml" + SamlSoapEndpoint.SERVLET_PATH +
				"/AssertionQueryService";
		
		DefaultClientConfiguration clientCfg = getClientCfg();
		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("mockPassword1");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		NameID localIssuer = new NameID("http://example-reqester", SAMLConstants.NFORMAT_ENTITY);
		
		EntityParam entityParam = new EntityParam(new IdentityTaV(X500Identity.ID, DEMO_SERVER_DN));
		SamlPreferences preferences = new SamlPreferences();
		SPSettings settings = new SPSettings();
		Map<String, Attribute> hidden = new HashMap<>();
		hidden.put("memberOf", null);
		Attribute fpa = FloatingPointAttribute.of("floatA", "/", 124.1);
		hidden.put("floatA", fpa);
		settings.setHiddenAttribtues(hidden);
		preferences.setSPSettings("http://example-reqester", settings);
		preferencesMan.setPreference(entityParam, SamlPreferences.ID, 
				JsonUtil.serialize(preferences.getSerializedConfiguration()));
		
		SAMLAttributeQueryClient client = new SAMLAttributeQueryClient(attrWSUrl, clientCfg);
		AttributeAssertionParser a = client.getAssertion(new NameID(DEMO_SERVER_DN, SAMLConstants.NFORMAT_DN),
				localIssuer);
		assertEquals(6, a.getAttributes().size()); //3 identities, 3 plain attributes
		ParsedAttribute a1 = a.getAttribute("stringA");
		assertNotNull(a1);
		assertEquals(0, a1.getStringValues().size());
		ParsedAttribute a2 = a.getAttribute("intA");
		assertNotNull(a2);
		assertEquals(1, a2.getStringValues().size());
		assertEquals("1", a2.getStringValues().get(0));
		ParsedAttribute a3 = a.getAttribute("floatA");
		assertNotNull(a3);
		assertEquals(2, a3.getStringValues().size());
		assertEquals("123.1", a3.getStringValues().get(0));
		assertEquals("14.2", a3.getStringValues().get(1));
		ParsedAttribute a4 = a.getAttribute("memberOf");
		assertNull(a4);
	}

	
	@Test
	public void gettingPreferencesOfMissingEntityShouldReturnDefaults()
	{
		EntityParam missing = new EntityParam(new IdentityTaV("foo", "bar"));
		SamlPreferences preferences = SamlPreferences.getPreferences(preferencesMan, 
				missing);
		
		assertThat(preferences, is(notNullValue()));
	}
}
