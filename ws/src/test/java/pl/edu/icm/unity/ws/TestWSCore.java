/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import javax.xml.ws.soap.SOAPFaultException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import eu.unicore.security.wsutil.client.WSClientFactory;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.mock.MockPasswordVerificatorFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.ws.mock.MockWSEndpointFactory;
import pl.edu.icm.unity.ws.mock.MockWSSEI;
import xmlbeans.org.oasis.saml2.assertion.NameIDDocument;


public class TestWSCore extends DBIntegrationTestBase
{
	public static final String AUTHENTICATOR_WS_PASS = "ApassWS";
	public static final String AUTHENTICATOR_WS_CERT = "AcertWS";
	public static final String AUTHENTICATION_FLOW = "flow1";
	public static final String AUTHENTICATION_FLOW_CERT_SECOND_FACTOR = "flow2";
	public static final String AUTHENTICATION_FLOW_OPTIN = "flow3";
	
	@Autowired
	private AuthenticatorManagement authnMan;
	
	@Autowired
	private AuthenticationFlowManagement authnFlowMan;
	
	@Test
	public void shouldBlockAccessAfterTooManyFailedLogins() throws Exception
	{
		initializeHTTPServer();
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);

		clientCfg.setHttpUser(DEF_USER);
		clientCfg.setHttpPassword("wrong");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		WSClientFactory factoryBad = new WSClientFactory(clientCfg);
		MockWSSEI wsProxyBad = factoryBad.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);
		clientCfg.setHttpPassword(DEF_PASSWORD);
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
	}
	
	@Test
	public void shouldRespectUserOptinAttr() throws Exception
	{
		initializeHTTPServer(AUTHENTICATION_FLOW_OPTIN);
		
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setHttpUser(DEF_USER);
		clientCfg.setHttpPassword(DEF_PASSWORD);
		WSClientFactory factory = new WSClientFactory(clientCfg);
		MockWSSEI wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		EntityWithCredential entity = identityResolver.resolveIdentity(DEF_USER, new String[] {UsernameIdentity.ID}, 
				MockPasswordVerificatorFactory.ID);
		authnFlowMan.setUserMFAOptIn(entity.getEntityId(), true);
		
		try
		{
			factory = new WSClientFactory(clientCfg);
			wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
					MockWSEndpointFactory.SERVLET_PATH);
			wsProxy.getAuthenticatedUser();
			fail("Managed to authenticate with sigle cred when USER_OPTIN flow policy is used, userOptin attr is set and second credential is not given");
		} catch (SOAPFaultException e)
		{
			//ok
		}
		
		authnFlowMan.setUserMFAOptIn(entity.getEntityId(), false);
		
		NameIDDocument ret = wsProxy.getAuthenticatedUser();
		assertEquals("[" + DEF_USER + "]", ret.getNameID().getStringValue());	
	}
	
	
	@Test
	public void shouldFailToAuthenticateWithWrongPassword() throws Exception
	{
		initializeHTTPServer();
		
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);

		clientCfg.setHttpUser(DEF_USER);
		clientCfg.setHttpPassword("wrong");
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		
		try
		{
			WSClientFactory factory = new WSClientFactory(clientCfg);
			MockWSSEI wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
					MockWSEndpointFactory.SERVLET_PATH);
			NameIDDocument retDoc = wsProxy.getAuthenticatedUser();
			fail("Managed to authenticate with wrong password: " + retDoc.xmlText());
		} catch (SOAPFaultException e)
		{
			//ok
		}
	}
	
	@Test
	public void shouldAuthenticateWithTLSCert() throws Exception
	{
		initializeHTTPServer();
		
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);

		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(false);
		WSClientFactory factory = new WSClientFactory(clientCfg);
		MockWSSEI wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);
		
		NameIDDocument ret = wsProxy.getAuthenticatedUser();
		assertEquals("[" + DEMO_SERVER_DN + "]", ret.getNameID().getStringValue());
	}

	@Test
	public void shouldAuthenticateWithPassword() throws Exception
	{
		initializeHTTPServer();
		
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);

		clientCfg.setHttpUser(DEF_USER);
		clientCfg.setHttpPassword(DEF_PASSWORD);
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		WSClientFactory factory = new WSClientFactory(clientCfg);
		MockWSSEI wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);
		NameIDDocument ret = wsProxy.getAuthenticatedUser();
		assertEquals("[" + DEF_USER + "]", ret.getNameID().getStringValue());
	}
	
	@Test
	public void shouldAuthenticateWithTLSCertWhenWrongPasswordProvided() throws Exception
	{
		initializeHTTPServer();
		
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);

		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpPassword("wrong");
		WSClientFactory factory = new WSClientFactory(clientCfg);
		MockWSSEI wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock"+
				MockWSEndpointFactory.SERVLET_PATH);
		NameIDDocument ret = wsProxy.getAuthenticatedUser();
		assertEquals("[" + DEMO_SERVER_DN + "]", ret.getNameID().getStringValue());
	}
	
	@Test
	public void shouldAuthenticateWith2Factors() throws Exception
	{
		initializeHTTPServer();
		
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);

		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpUser("user2");
		clientCfg.setHttpPassword("mockPassword2");
		
		WSClientFactory factory = new WSClientFactory(clientCfg);
		MockWSSEI wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock2"+
				MockWSEndpointFactory.SERVLET_PATH);
		NameIDDocument ret = wsProxy.getAuthenticatedUser();
		assertEquals("[" + DEMO_SERVER_DN + ", user2]", ret.getNameID().getStringValue());
	}
	
	@Test
	public void shouldFailToAuthenticateWithPasswordWhen2FactorsRequired() throws Exception
	{
		initializeHTTPServer();
		
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		clientCfg.setHttpUser("user2");
		clientCfg.setHttpPassword("mockPassword2");

		try
		{
			WSClientFactory factory = new WSClientFactory(clientCfg);
			MockWSSEI wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock2"+
					MockWSEndpointFactory.SERVLET_PATH);
			wsProxy.getAuthenticatedUser();
			fail("Managed to authenticate with single cred when 2 req");
		} catch (SOAPFaultException e)
		{
			//ok
		}
	}
	
	@Test
	public void shouldFailToAuthenticateWithTLSCertWhen2FactorsRequired() throws Exception
	{
		initializeHTTPServer();
		
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(true);
		clientCfg.setHttpAuthn(false);

		try
		{
			WSClientFactory factory = new WSClientFactory(clientCfg);
			MockWSSEI wsProxy = factory.createPlainWSProxy(MockWSSEI.class, "https://localhost:53456/mock2"+
					MockWSEndpointFactory.SERVLET_PATH);
			wsProxy.getAuthenticatedUser();
			fail("Managed to authenticate with single cred when 2 req");
		} catch (SOAPFaultException e)
		{
			//ok
		}
	}
	
	private void initializeHTTPServer() throws Exception
	{
		initializeHTTPServer(AUTHENTICATION_FLOW);
	}
	
	private void initializeHTTPServer(String authnFlow) throws Exception
	{
		setupAuth();
		createUsers();
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				5, 1, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);
		
		
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", Lists.newArrayList(authnFlow), "", realm.getName());
		endpointMan.deploy(MockWSEndpointFactory.NAME, "endpoint1", "/mock", cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		EndpointConfiguration cfg2 = new EndpointConfiguration(new I18nString("endpoint2"),
				"desc", Lists.newArrayList(AUTHENTICATION_FLOW_CERT_SECOND_FACTOR), "", realm.getName());
		endpointMan.deploy(MockWSEndpointFactory.NAME, "endpoint2", "/mock2", cfg2);
		
		httpServer.start();
	}
	
	protected void createUsers() throws Exception
	{
		createUsernameUser(DEF_USER, null, DEF_PASSWORD, CRED_REQ_PASS);
		createCertUser();
	}
	
	protected void setupAuth() throws Exception
	{
		setupPasswordAuthn();
		setupPasswordAndCertAuthn();
		authnMan.createAuthenticator(AUTHENTICATOR_WS_CERT, "certificate", "", "credential2");
		authnMan.createAuthenticator(AUTHENTICATOR_WS_PASS, "password", "", "credential1");
		
		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				AUTHENTICATION_FLOW, Policy.NEVER,
				Sets.newHashSet(AUTHENTICATOR_WS_PASS, AUTHENTICATOR_WS_CERT)));
		
		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				AUTHENTICATION_FLOW_CERT_SECOND_FACTOR, Policy.REQUIRE,
				Sets.newHashSet(AUTHENTICATOR_WS_PASS), Lists.newArrayList(AUTHENTICATOR_WS_CERT)));

		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				AUTHENTICATION_FLOW_OPTIN, Policy.USER_OPTIN,
				Sets.newHashSet(AUTHENTICATOR_WS_PASS, AUTHENTICATOR_WS_CERT)));
		
	}
}
