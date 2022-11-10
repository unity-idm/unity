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

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
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
		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		assertEquals(1, endpoints.size());

		httpServer.start();
	}
	
	@Test
	public void tokenIsReturned() throws Exception
	{
		HttpGet get = new HttpGet("/jwt/token");
		ClassicHttpResponse response = executeWithLC(get);
		assertEquals(new StatusLine(response).toString(), 200, response.getCode());
		String token = EntityUtils.toString(response.getEntity());
		System.out.println("Received token: " + token);
	}	

	@Test
	public void tokenIsNotReturnedWithoutAuthn() throws Exception
	{
		HttpGet get = new HttpGet("/jwt/token");
		ClassicHttpResponse response = execute(get);
		assertEquals(new StatusLine(response).toString(), 500, response.getCode());
	}	

	@Test
	public void tokenCanBeRefreshed() throws Exception
	{
		HttpGet get = new HttpGet("/jwt/token");
		ClassicHttpResponse response = executeWithLC(get);
		String token = EntityUtils.toString(response.getEntity());
		
		HttpPost post = new HttpPost("/jwt/refreshToken");
		post.setHeader("Authorization", "Bearer " + token);
		post.setEntity(new StringEntity(token));
		response = execute(post);
		String token2 = EntityUtils.toString(response.getEntity());
		assertEquals(new StatusLine(response).toString(), 200, response.getCode());
		assertThat(token2, is(not(token)));
	}

	@Test
	public void invalidatedTokenCantBeRefreshed() throws Exception
	{
		HttpGet get = new HttpGet("/jwt/token");
		ClassicHttpResponse response = executeWithLC(get);
		String token = EntityUtils.toString(response.getEntity());
		
		HttpPost post = new HttpPost("/jwt/invalidateToken");
		post.setHeader("Authorization", "Bearer " + token);
		post.setEntity(new StringEntity(token));
		response = execute(post);
		assertEquals(new StatusLine(response).toString(), 204, response.getCode());

		HttpPost post2 = new HttpPost("/jwt/refreshToken");
		post2.setHeader("Authorization", "Bearer " + token);
		post2.setEntity(new StringEntity(token));
		response = execute(post2);
		assertEquals(new StatusLine(response).toString(), 410, response.getCode());
	}
	
	@Test
	public void expiredTokenCantBeUsedForAuthenticationOfRequest() throws Exception
	{
		HttpGet get = new HttpGet("/jwt/token");
		ClassicHttpResponse response = executeWithLC(get);
		String token = EntityUtils.toString(response.getEntity());
		long start = System.currentTimeMillis();

		Thread.sleep(2001-(System.currentTimeMillis()-start));
		HttpPost post = new HttpPost("/jwt/refreshToken");
		post.setHeader("Authorization", "Bearer " + token);
		post.setEntity(new StringEntity(token));
		response = execute(post);
		
		assertEquals(new StatusLine(response).toString(), 500, response.getCode());
	}
	

	private ClassicHttpResponse executeWithLC(ClassicHttpRequest request) throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);
		HttpClientContext localcontext = getClientContext(host);
		return client.executeOpen(host, request, localcontext);
	}
	
	private ClassicHttpResponse execute(ClassicHttpRequest request) throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);
		return client.executeOpen(host, request, null);
	}
}
