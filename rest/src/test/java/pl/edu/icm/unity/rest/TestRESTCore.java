/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import pl.edu.icm.unity.rest.authn.AuthenticationInterceptor;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * Tests REST integration basic. Not that more detailed tests of {@link AuthenticationInterceptor}
 * are in the ws module for historical reasons.  
 * @author K. Benedyczak
 */
public class TestRESTCore extends TestRESTBase
{
	@Test
	public void test() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUser("Regular User");
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 100, -1, 600);
		realmsMan.addRealm(realm);
		
		List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
		authnCfg.add(new AuthenticatorSet(Collections.singleton(AUTHENTICATOR_REST_PASS)));
		endpointMan.deploy(MockRESTEndpointFactory.NAME, 
				"endpoint1", new I18nString("endpoint1"),
				"/mock", "desc", authnCfg, "", realm.getName());
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		httpServer.start();

		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);
		
		HttpGet get = new HttpGet("/mock/mock-rest/test/r1");
		HttpResponse response = client.execute(host, get, localcontext);
		assertEquals(response.getStatusLine().toString(), 200, response.getStatusLine().getStatusCode());
		System.out.println(EntityUtils.toString(response.getEntity()));
		
		//no password, should fail.
		HttpResponse response2 = client.execute(host, get);
		assertEquals(response2.getStatusLine().toString(), 500, response2.getStatusLine().getStatusCode());
	}
}
