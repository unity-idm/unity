/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.soap.SOAPFaultException;

import org.junit.Test;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.ws.mock.MockWSEndpointFactory;
import pl.edu.icm.unity.ws.mock.MockWSSEI;
import xmlbeans.org.oasis.saml2.assertion.NameIDDocument;
import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.security.wsutil.client.WSClientFactory;
import eu.unicore.util.httpclient.DefaultClientConfiguration;


public class TestWSCore extends DBIntegrationTestBase
{
	public static final String AUTHENTICATOR_WS_PASS = "ApassWS";
	public static final String AUTHENTICATOR_WS_CERT = "AcertWS";
	
	@Test
	public void testBlockAccess() throws Exception
	{
		setupAuth();
		createUsers();
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				5, 1, -1, 600);
		realmsMan.addRealm(realm);
		
		List<AuthenticationOptionDescription> authnCfg = new ArrayList<AuthenticationOptionDescription>();
		authnCfg.add(new AuthenticationOptionDescription(AUTHENTICATOR_WS_PASS));
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", authnCfg, "", realm.getName());
		endpointMan.deploy(MockWSEndpointFactory.NAME, "endpoint1", "/mock", cfg);

		httpServer.start();
		
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(new KeystoreCredential("src/test/resources/demoKeystore.p12", 
				"the!uvos".toCharArray(), "the!uvos".toCharArray(), "uvos", "PKCS12"));
		clientCfg.setValidator(new KeystoreCertChainValidator("src/test/resources/demoTruststore.jks", 
				"unicore".toCharArray(), "JKS", -1));
		clientCfg.setSslEnabled(true);

		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("wrong");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		WSClientFactory factoryBad = new WSClientFactory(clientCfg);
		MockWSSEI wsProxyBad = factoryBad.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);
		clientCfg.setHttpPassword("mockPassword1");
		WSClientFactory factoryOK = new WSClientFactory(clientCfg);
		MockWSSEI wsProxyOK = factoryOK.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);

		wsProxyOK.getAuthenticatedUser();
		
		//no blocking after this
		for (int i=0; i<4; i++)
		{
			try
			{
				NameIDDocument retDoc = wsProxyBad.getAuthenticatedUser();
				fail("Managed to authenticate with wrong password: " + retDoc.xmlText());
			} catch (SOAPFaultException e)
			{
				//ok
			}
		}
		
		//reset
		wsProxyOK.getAuthenticatedUser();
		
		//this should trigger blockade
		for (int i=0; i<5; i++)
		{
			try
			{
				NameIDDocument retDoc = wsProxyBad.getAuthenticatedUser();
				fail("Managed to authenticate with wrong password: " + retDoc.xmlText());
			} catch (SOAPFaultException e)
			{
				//ok
			}
		}
		
		//this should be blocked
		try
		{
			wsProxyOK.getAuthenticatedUser();
			fail("Managed to authenticate with correct password when access should be blocked");
		} catch (SOAPFaultException e)
		{
			//ok
		}
		
//		Thread.sleep(1100);
//		//reset
//		wsProxyOK.getAuthenticatedUser();
	}
	
	@Test
	public void test() throws Exception
	{
		setupAuth();
		createUsers();
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 100, -1, 600);
		realmsMan.addRealm(realm);
		
		
		List<AuthenticationOptionDescription> authnCfg = new ArrayList<AuthenticationOptionDescription>();
		authnCfg.add(new AuthenticationOptionDescription(AUTHENTICATOR_WS_PASS));
		authnCfg.add(new AuthenticationOptionDescription(AUTHENTICATOR_WS_CERT));
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", authnCfg, "", realm.getName());
		endpointMan.deploy(MockWSEndpointFactory.NAME, "endpoint1", "/mock", cfg);
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		httpServer.start();
		
		
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(new KeystoreCredential("src/test/resources/demoKeystore.p12", 
				"the!uvos".toCharArray(), "the!uvos".toCharArray(), "uvos", "PKCS12"));
		clientCfg.setValidator(new KeystoreCertChainValidator("src/test/resources/demoTruststore.jks", 
				"unicore".toCharArray(), "JKS", -1));
		clientCfg.setSslEnabled(true);

		clientCfg.setHttpUser("user1");
		clientCfg.setHttpPassword("mockPassword1");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		WSClientFactory factory = new WSClientFactory(clientCfg);
		MockWSSEI wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);
		NameIDDocument ret = wsProxy.getAuthenticatedUser();
		assertEquals("[user1]", ret.getNameID().getStringValue());
		
		try
		{
			clientCfg.setHttpPassword("wrong");
			factory = new WSClientFactory(clientCfg);
			wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
					MockWSEndpointFactory.SERVLET_PATH);
			NameIDDocument retDoc = wsProxy.getAuthenticatedUser();
			fail("Managed to authenticate with wrong password: " + retDoc.xmlText());
		} catch (SOAPFaultException e)
		{
			//ok
		}
		
		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(false);
		factory = new WSClientFactory(clientCfg);
		wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);
		ret = wsProxy.getAuthenticatedUser();
		assertEquals("[CN=Test UVOS,O=UNICORE,C=EU]", ret.getNameID().getStringValue());

		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpPassword("wrong");
		factory = new WSClientFactory(clientCfg);
		wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);
		ret = wsProxy.getAuthenticatedUser();
		assertEquals("[CN=Test UVOS,O=UNICORE,C=EU]", ret.getNameID().getStringValue());

		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpPassword("mockPassword1");
		factory = new WSClientFactory(clientCfg);
		wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);
		ret = wsProxy.getAuthenticatedUser();
		assertEquals("[user1]", ret.getNameID().getStringValue());
		

		List<AuthenticationOptionDescription> authnCfg2 = new ArrayList<AuthenticationOptionDescription>();
		authnCfg2.add(new AuthenticationOptionDescription(AUTHENTICATOR_WS_PASS, AUTHENTICATOR_WS_CERT));
		EndpointConfiguration cfg2 = new EndpointConfiguration(new I18nString("endpoint2"),
				"desc", authnCfg2, "", realm.getName());
		endpointMan.deploy(MockWSEndpointFactory.NAME, "endpoint2", "/mock2", cfg2);
		
		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpUser("user2");
		clientCfg.setHttpPassword("mockPassword2");
		factory = new WSClientFactory(clientCfg);
		wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock2"+
				MockWSEndpointFactory.SERVLET_PATH);
		ret = wsProxy.getAuthenticatedUser();
		assertEquals("[CN=Test UVOS,O=UNICORE,C=EU, user2]", ret.getNameID().getStringValue());

		try
		{
			clientCfg.setSslAuthn(false);
			factory = new WSClientFactory(clientCfg);
			wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock2"+
					MockWSEndpointFactory.SERVLET_PATH);
			wsProxy.getAuthenticatedUser();
			fail("Managed to authenticate with single cred when 2 req");
		} catch (SOAPFaultException e)
		{
			//ok
		}

		try
		{
			clientCfg.setSslAuthn(true);
			clientCfg.setHttpAuthn(false);
			factory = new WSClientFactory(clientCfg);
			wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock2"+
					MockWSEndpointFactory.SERVLET_PATH);
			wsProxy.getAuthenticatedUser();
			fail("Managed to authenticate with single cred when 2 req");
		} catch (SOAPFaultException e)
		{
			//ok
		}
		
	}
	
	protected void createUsers() throws Exception
	{
		createUsernameUser(null);
		createCertUser();
	}
	
	protected void setupAuth() throws Exception
	{
		setupPasswordAuthn();
		setupPasswordAndCertAuthn();
		authnMan.createAuthenticator(AUTHENTICATOR_WS_CERT, "certificate with cxf-certificate", null, "", "credential2");
		authnMan.createAuthenticator(AUTHENTICATOR_WS_PASS, "password with cxf-httpbasic", null, "", "credential1");
	}
}
