/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.rest.jwt.endpoint.JWTManagementEndpoint;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

/**
 * Tests JWT management and authentication
 * @author K. Benedyczak
 */
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServerRest.conf" })
public class TestJWTAuthentication extends TestRESTBase
{
	private static final String JWT_CONFIG = "unity.jwtauthn.tokenTtl=2\n"
			+ "unity.jwtauthn.credential=MAIN\n";
	
	@Before
	public void setup() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUserWithRole("Regular User");
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 100, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);
		
		authnMan.createAuthenticator("Ajwt", "jwt", JWT_CONFIG, null);
		
		authFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER,
				Sets.newHashSet(AUTHENTICATOR_REST_PASS,"Ajwt")));
		
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("jwtMan"), "desc", Lists.newArrayList("flow1"), 
				JWT_CONFIG, realm.getName());
		endpointMan.deploy(JWTManagementEndpoint.NAME, "jwtMan", "/jwt", cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		httpServer.start();
	}
	
	@Test
	public void tokenIsReturned() throws Exception
	{
		HttpGet get = new HttpGet("/jwt/token");
		HttpResponse response = executeWithLC(get);
		assertEquals(response.getStatusLine().toString(), 200, response.getStatusLine().getStatusCode());
		String token = EntityUtils.toString(response.getEntity());
		System.out.println("Received token: " + token);
	}	

	@Test
	public void tokenIsNotReturnedWithoutAuthn() throws Exception
	{
		HttpGet get = new HttpGet("/jwt/token");
		HttpResponse response2 = execute(get);
		assertEquals(response2.getStatusLine().toString(), 500, response2.getStatusLine().getStatusCode());
	}	

	@Test
	public void tokenCanBeRefreshed() throws Exception
	{
		HttpGet get = new HttpGet("/jwt/token");
		HttpResponse response = executeWithLC(get);
		String token = EntityUtils.toString(response.getEntity());
		
		HttpPost post = new HttpPost("/jwt/refreshToken");
		post.setHeader("Authorization", "Bearer " + token);
		post.setEntity(new StringEntity(token));
		HttpResponse response3 = execute(post);
		String token2 = EntityUtils.toString(response3.getEntity());
		assertEquals(response3.getStatusLine().toString(), 200, response3.getStatusLine().getStatusCode());
		assertThat(token2, is(not(token)));
	}

	@Test
	public void invalidatedTokenCantBeRefreshed() throws Exception
	{
		HttpGet get = new HttpGet("/jwt/token");
		HttpResponse response = executeWithLC(get);
		String token = EntityUtils.toString(response.getEntity());
		
		HttpPost post2 = new HttpPost("/jwt/invalidateToken");
		post2.setHeader("Authorization", "Bearer " + token);
		post2.setEntity(new StringEntity(token));
		HttpResponse response4 = execute(post2);
		assertEquals(response4.getStatusLine().toString(), 204, response4.getStatusLine().getStatusCode());
		
		HttpPost post3 = new HttpPost("/jwt/refreshToken");
		post3.setHeader("Authorization", "Bearer " + token);
		post3.setEntity(new StringEntity(token));
		HttpResponse response5 = execute(post3);
		assertEquals(response5.getStatusLine().toString(), 410, response5.getStatusLine().getStatusCode());
	}
	
	@Test
	public void expiredTokenCantBeUsedForAuthenticationOfRequest() throws Exception
	{
		HttpGet get = new HttpGet("/jwt/token");
		HttpResponse response = executeWithLC(get);
		String token = EntityUtils.toString(response.getEntity());
		long start = System.currentTimeMillis();

		Thread.sleep(2001-(System.currentTimeMillis()-start));
		HttpPost post4 = new HttpPost("/jwt/refreshToken");
		post4.setHeader("Authorization", "Bearer " + token);
		post4.setEntity(new StringEntity(token));
		HttpResponse response6 = execute(post4);
		
		assertEquals(response6.getStatusLine().toString(), 500, response6.getStatusLine().getStatusCode());
	}
	

	private HttpResponse executeWithLC(HttpRequest request) throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(host);
		return client.execute(host, request, localcontext);
	}
	
	private HttpResponse execute(HttpRequest request) throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		return client.execute(host, request);
	}
}
