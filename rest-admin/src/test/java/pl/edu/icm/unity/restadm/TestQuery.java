/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

public class TestQuery extends DBIntegrationTestBase
{
	@Test
	public void testQuery() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUser("System Manager");
		deployEndpoint();
		
		DefaultHttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		BasicHttpContext localcontext = getClientContext(client, host);

		HttpGet get = new HttpGet("/restadm/entity/1/groups");
		HttpResponse response = client.execute(host, get, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		System.out.println("User's groups:\n" + contents);
	}
	
	protected void deployEndpoint() throws Exception
	{
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 100, -1, 600);
		realmsMan.addRealm(realm);
		
		List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
		authnCfg.add(new AuthenticatorSet(Collections.singleton("ApassREST")));
		endpointMan.deploy(RESTAdminEndpointFactory.NAME, 
				"restAdmin", "/restadm", "desc", authnCfg, "", realm.getName());
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		httpServer.start();
	}
	
	protected BasicHttpContext getClientContext(DefaultHttpClient client, HttpHost host)
	{
		client.getCredentialsProvider().setCredentials(
				new AuthScope(host.getHostName(), host.getPort()),
				new UsernamePasswordCredentials("user1", "mockPassword1"));
		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(host, basicAuth);
		BasicHttpContext localcontext = new BasicHttpContext();
		localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);
		return localcontext;
	}
	
	protected DefaultHttpClient getClient() throws Exception
	{
		DefaultClientConfiguration clientCfg = new DefaultClientConfiguration();
		clientCfg.setValidator(new KeystoreCertChainValidator("src/test/resources/demoTruststore.jks", 
				"unicore".toCharArray(), "JKS", -1));
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(false);
		return (DefaultHttpClient) HttpUtils.createClient("https://localhost:53456", clientCfg);
	}
	
	@Override
	protected void setupPasswordAuthn() throws Exception
	{
		super.setupPasswordAuthn();
		authnMan.createAuthenticator("ApassREST", "password with rest-httpbasic", 
				null, "", "credential1");
	}
}
