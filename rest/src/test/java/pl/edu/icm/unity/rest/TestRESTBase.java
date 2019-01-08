/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;

import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

public abstract class TestRESTBase extends DBIntegrationTestBase
{
	public static final String AUTHENTICATOR_REST_PASS = "ApassREST";
	public static final String AUTHENTICATION_FLOW_PASS = "ApassRESTFlow";
	public static final String AUTHENTICATOR_REST_CERT = "AcertREST";
	
	@Autowired
	protected AuthenticatorManagement authnMan;
	
	@Autowired
	protected AuthenticationFlowManagement authFlowMan;
	
	
	protected HttpClientContext getClientContext(HttpHost host)
	{
		return getClientContext(host, DEF_USER, DEF_PASSWORD);
	}
		
	protected HttpClientContext getClientContext(HttpHost host, String user, String pass)
	{
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(host.getHostName(), host.getPort()),
				new UsernamePasswordCredentials(user, pass));

		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(host, basicAuth);

		HttpClientContext context = HttpClientContext.create();
		context.setCredentialsProvider(credsProvider);
		context.setAuthCache(authCache);
		return context;
	}
	
	protected HttpClient getClient() throws Exception
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setCredential(getDemoCredential());
		clientCfg.setValidator(getDemoValidator());
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		return HttpUtils.createClient("https://localhost:53456", clientCfg);
	}
	
	@Override
	protected void setupPasswordAuthn() throws EngineException
	{
		super.setupPasswordAuthn();
		
		Set<String> firstFactor = new HashSet<>();
		firstFactor.add(AUTHENTICATOR_REST_PASS);
		authnMan.createAuthenticator(AUTHENTICATOR_REST_PASS, "password", "", "credential1");
		authFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(AUTHENTICATION_FLOW_PASS, Policy.NEVER, firstFactor));
	}	
	
	
	protected void deployEndpoint(String endpointTypeName, String name, String context) throws Exception
	{
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 100, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);

		List<String> authnCfg = new ArrayList<>();
		authnCfg.add(AUTHENTICATION_FLOW_PASS);	
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString(name),
				"desc", authnCfg, "", realm.getName());
		endpointMan.deploy(endpointTypeName, name, context, cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		httpServer.start();
	}
}
