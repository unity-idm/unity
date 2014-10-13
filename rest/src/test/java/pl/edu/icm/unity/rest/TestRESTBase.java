/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;

import eu.emi.security.authn.x509.impl.KeystoreCertChainValidator;
import eu.emi.security.authn.x509.impl.KeystoreCredential;
import eu.unicore.util.httpclient.DefaultClientConfiguration;
import eu.unicore.util.httpclient.HttpUtils;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;

public abstract class TestRESTBase extends DBIntegrationTestBase
{
	public static final String AUTHENTICATOR_REST_PASS = "ApassREST";
	public static final String AUTHENTICATOR_REST_CERT = "AcertREST";
	
	
	
	protected BasicHttpContext getClientContext(DefaultHttpClient client, HttpHost host)
	{
		return getClientContext(client, host, "user1","mockPassword1");
	}
		
	protected BasicHttpContext getClientContext(DefaultHttpClient client, HttpHost host, String user, String pass)
	{
		client.getCredentialsProvider().setCredentials(
				new AuthScope(host.getHostName(), host.getPort()),
				new UsernamePasswordCredentials(user, pass));
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
		clientCfg.setCredential(new KeystoreCredential("src/test/resources/demoKeystore.p12", 
				"the!uvos".toCharArray(), "the!uvos".toCharArray(), "uvos", "PKCS12"));
		clientCfg.setValidator(new KeystoreCertChainValidator("src/test/resources/demoTruststore.jks", 
				"unicore".toCharArray(), "JKS", -1));
		clientCfg.setSslEnabled(true);
		clientCfg.setSslAuthn(false);
		clientCfg.setHttpAuthn(true);
		return (DefaultHttpClient) HttpUtils.createClient("https://localhost:53456", clientCfg);
	}
	
	protected void setupPasswordAuthn() throws Exception
	{
		super.setupPasswordAuthn();
		authnMan.createAuthenticator(AUTHENTICATOR_REST_PASS, "password with rest-httpbasic", 
				null, "", "credential1");
	}
}
