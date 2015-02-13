/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpointFactory;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * Tests JWT management and authentication
 * @author K. Benedyczak
 */
public class TestJWTAuthentication extends TestRESTBase
{
	private static final String JWT_CONFIG = "unity.jwtauthn.tokenTtl=2\n"
			+ "unity.jwtauthn.credential=MAIN\n";
	
	@Test
	public void testJWT() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUser("Regular User");
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 100, -1, 600);
		realmsMan.addRealm(realm);
		
		authnMan.createAuthenticator("Ajwt", "jwt with rest-jwt", JWT_CONFIG, null, null);
		
		List<AuthenticationOptionDescription> authnCfg = new ArrayList<AuthenticationOptionDescription>();
		authnCfg.add(new AuthenticationOptionDescription(Collections.singleton(AUTHENTICATOR_REST_PASS)));
		authnCfg.add(new AuthenticationOptionDescription(Collections.singleton("Ajwt")));
		endpointMan.deploy(JWTManagementEndpointFactory.NAME, 
				"jwtMan", new I18nString("jwtMan"), "/jwt", "desc", authnCfg, 
				JWT_CONFIG, realm.getName());
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		httpServer.start();
		
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(client, host);
		
		HttpGet get = new HttpGet("/jwt/token");
		HttpResponse response = client.execute(host, get, localcontext);
		assertEquals(response.getStatusLine().toString(), 200, response.getStatusLine().getStatusCode());
		String token = EntityUtils.toString(response.getEntity());
		System.out.println("Received token: " + token);
		long start = System.currentTimeMillis();

		HttpResponse response2 = client.execute(host, get);
		assertEquals(response2.getStatusLine().toString(), 500, response2.getStatusLine().getStatusCode());
		
		
		HttpPost post = new HttpPost("/jwt/refreshToken");
		post.setHeader("Authorization", "Bearer " + token);
		post.setEntity(new StringEntity(token));
		HttpResponse response3 = client.execute(host, post);
		String token2 = EntityUtils.toString(response3.getEntity());
		assertEquals(response3.getStatusLine().toString(), 200, response3.getStatusLine().getStatusCode());
		
		HttpPost post2 = new HttpPost("/jwt/invalidateToken");
		post2.setHeader("Authorization", "Bearer " + token2);
		post2.setEntity(new StringEntity(token2));
		HttpResponse response4 = client.execute(host, post2);
		assertEquals(response4.getStatusLine().toString(), 204, response4.getStatusLine().getStatusCode());
		
		HttpPost post3 = new HttpPost("/jwt/refreshToken");
		post3.setHeader("Authorization", "Bearer " + token2);
		post3.setEntity(new StringEntity(token2));
		HttpResponse response5 = client.execute(host, post3);
		assertEquals(response5.getStatusLine().toString(), 410, response5.getStatusLine().getStatusCode());
		
		Thread.sleep(2001-(System.currentTimeMillis()-start));
		HttpPost post4 = new HttpPost("/jwt/refreshToken");
		post4.setHeader("Authorization", "Bearer " + token);
		post4.setEntity(new StringEntity(token));
		HttpResponse response6 = client.execute(host, post4);
		assertEquals(response6.getStatusLine().toString(), 500, response6.getStatusLine().getStatusCode());
	}
}
