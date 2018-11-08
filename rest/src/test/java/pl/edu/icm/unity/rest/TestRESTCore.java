/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.rest.authn.AuthenticationInterceptor;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;

/**
 * Tests REST integration basic. Note that more detailed tests of {@link AuthenticationInterceptor}
 * are in the ws module for historical reasons.  
 * @author K. Benedyczak
 */
@TestPropertySource(properties = { "unityConfig: src/test/resources/unityServerRest.conf" })
public class TestRESTCore extends TestRESTBase
{
	private static final String ALLOWED_ORIGIN1 = "http://someorigin.com";
	private static final String ALLOWED_ORIGIN2 = "http://someorigin.com2";
	private static final String ALLOWED_HEADER = "authorization";
	
	@Before
	public void configureEndpoint() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUserWithRole("Regular User");
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 100, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);
		
		authFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER,
				Sets.newHashSet(AUTHENTICATOR_REST_PASS)));
		
		Properties config = new Properties();
		config.setProperty(RESTEndpointProperties.PREFIX+RESTEndpointProperties.ENABLED_CORS_ORIGINS + "1", 
				ALLOWED_ORIGIN1);
		config.setProperty(RESTEndpointProperties.PREFIX+RESTEndpointProperties.ENABLED_CORS_ORIGINS + "2", 
				ALLOWED_ORIGIN2);
		config.setProperty(RESTEndpointProperties.PREFIX+RESTEndpointProperties.ENABLED_CORS_HEADERS + "1", 
				ALLOWED_HEADER);
		StringWriter writer = new StringWriter();
		config.store(writer, "");
		
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"),
				"desc", Lists.newArrayList("flow1"), writer.toString(), realm.getName());
		endpointMan.deploy(MockRESTEndpoint.NAME, "endpoint1", "/mock", cfg);
		httpServer.start();
	}

	
	@Test
	public void basicGetIsServed() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(host);
		
		HttpGet get = new HttpGet("/mock/mock-rest/test/r1");
		HttpResponse response = client.execute(host, get, localcontext);
		
		System.out.println(EntityUtils.toString(response.getEntity()));
		assertEquals(response.getStatusLine().toString(), 200, response.getStatusLine().getStatusCode());
	}
	
	@Test
	public void requestNotAuthenticatedIsForbidden() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpGet get = new HttpGet("/mock/mock-rest/test/r1");

		//no password, should fail.
		HttpResponse response2 = client.execute(host, get);

		assertThat(response2.getStatusLine().getStatusCode(), is(Status.FORBIDDEN.getStatusCode()));
	}
	
	@Test
	public void exceptionIsMappedToHTTPError() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(host);
		
		HttpGet get = new HttpGet("/mock/mock-rest/test/r1/exception");
		HttpResponse response = client.execute(host, get, localcontext);
		String entity = EntityUtils.toString(response.getEntity());
		System.out.println(entity);
		assertThat(response.getStatusLine().getStatusCode(), is(Status.FORBIDDEN.getStatusCode()));
		assertThat(response.getEntity().getContentType().getValue(), is(MediaType.APPLICATION_JSON));
		assertThat(entity, containsString("Test exception"));
	}
	
	@Test
	public void allowedCorsOriginIsAccepted() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(host);
		HttpOptions preflight = new HttpOptions("/mock/mock-rest/test/r1");
		preflight.addHeader("Origin", ALLOWED_ORIGIN2);
		preflight.addHeader("Access-Control-Request-Method", "PUT");
		
		HttpResponse response = client.execute(host, preflight, localcontext);
		
		assertCorsAllowed(response);
	}

	@Test
	public void allowedCorsHeaderIsAccepted() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(host);
		HttpOptions preflight = new HttpOptions("/mock/mock-rest/test/r1");
		preflight.addHeader("Origin", ALLOWED_ORIGIN2);
		preflight.addHeader("Access-Control-Request-Method", "PUT");
		preflight.addHeader("Access-Control-Request-Headers", ALLOWED_HEADER);
		
		HttpResponse response = client.execute(host, preflight, localcontext);
		
		assertCorsAllowed(response);
	}
	
	private void assertCorsAllowed(HttpResponse response)
	{
		assertEquals(response.getStatusLine().toString(), 200, response.getStatusLine().getStatusCode());
		System.out.println(Arrays.toString(response.getAllHeaders()));
		assertThat(response.getHeaders("Access-Control-Allow-Origin"), is(notNullValue()));
		assertThat(response.getHeaders("Access-Control-Allow-Origin").length, is(1));
		assertThat(response.getHeaders("Access-Control-Allow-Origin")[0].getValue(), is(ALLOWED_ORIGIN2));
		assertThat(response.getHeaders("Access-Control-Allow-Methods"), is(notNullValue()));
		assertThat(response.getHeaders("Access-Control-Allow-Methods").length, is(1));
		assertThat(response.getHeaders("Access-Control-Allow-Methods")[0].getValue(), allOf(
				containsString("GET"),
				containsString("POST"),
				containsString("DELETE"),
				containsString("PUT")));
	}
	
	@Test
	public void notAllowedCorsOriginIsNotAccepted() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(host);
		HttpOptions preflight = new HttpOptions("/mock/mock-rest/test/r1");
		preflight.addHeader("Origin", "http://notAllowedOrigin.com");
		
		HttpResponse response = client.execute(host, preflight, localcontext);
		
		assertEquals(response.getStatusLine().toString(), 200, response.getStatusLine().getStatusCode());
		System.out.println(Arrays.toString(response.getAllHeaders()));
		assertThat(response.getHeaders("Access-Control-Allow-Origin"), is(notNullValue()));
		assertThat(response.getHeaders("Access-Control-Allow-Origin").length, is(0));
		assertThat(response.getHeaders("Access-Control-Allow-Methods"), is(notNullValue()));
		assertThat(response.getHeaders("Access-Control-Allow-Methods").length, is(0));
	}
	
	@Test
	public void notAllowedCorsHeaderIsNotAccepted() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("localhost", 53456, "https");
		HttpContext localcontext = getClientContext(host);
		HttpOptions preflight = new HttpOptions("/mock/mock-rest/test/r1");
		preflight.addHeader("Origin", ALLOWED_ORIGIN2);
		preflight.addHeader("Access-Control-Request-Method", "PUT");
		preflight.addHeader("Access-Control-Request-Headers", "X-notAllowed");
		
		HttpResponse response = client.execute(host, preflight, localcontext);
		
		assertEquals(response.getStatusLine().toString(), 200, response.getStatusLine().getStatusCode());
		System.out.println(Arrays.toString(response.getAllHeaders()));
		assertThat(response.getHeaders("Access-Control-Allow-Origin"), is(notNullValue()));
		assertThat(response.getHeaders("Access-Control-Allow-Origin").length, is(0));
		assertThat(response.getHeaders("Access-Control-Allow-Methods"), is(notNullValue()));
		assertThat(response.getHeaders("Access-Control-Allow-Methods").length, is(0));
	}
}
