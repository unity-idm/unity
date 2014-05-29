/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.ws.soap.SOAPFaultException;

import org.junit.Test;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.security.wsutil.client.WSClientFactory;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.ws.mock.MockWSEndpointFactory;
import pl.edu.icm.unity.ws.mock.MockWSSEI;
import xmlbeans.org.oasis.saml2.assertion.NameIDDocument;


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
		
		List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
		authnCfg.add(new AuthenticatorSet(Collections.singleton(AUTHENTICATOR_WS_PASS)));
		endpointMan.deploy(MockWSEndpointFactory.NAME, "endpoint1", "/mock", "desc", authnCfg, "", realm.getName());

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
		
		
		List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
		authnCfg.add(new AuthenticatorSet(Collections.singleton(AUTHENTICATOR_WS_PASS)));
		authnCfg.add(new AuthenticatorSet(Collections.singleton(AUTHENTICATOR_WS_CERT)));
		endpointMan.deploy(MockWSEndpointFactory.NAME, "endpoint1", "/mock", "desc", authnCfg, "", realm.getName());
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
		

		List<AuthenticatorSet> authnCfg2 = new ArrayList<AuthenticatorSet>();
		Set<String> setC = new HashSet<String>();
		setC.add(AUTHENTICATOR_WS_PASS);
		setC.add(AUTHENTICATOR_WS_CERT);
		authnCfg2.add(new AuthenticatorSet(setC));
		endpointMan.deploy(MockWSEndpointFactory.NAME, "endpoint2", "/mock2", "desc",
				authnCfg2, "", realm.getName());
		
		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpUser("user2");
		clientCfg.setHttpPassword("mockPassword2");
		factory = new WSClientFactory(clientCfg);
		wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock2"+
				MockWSEndpointFactory.SERVLET_PATH);
		ret = wsProxy.getAuthenticatedUser();
		assertEquals("[user2, CN=Test UVOS,O=UNICORE,C=EU]", ret.getNameID().getStringValue());

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
