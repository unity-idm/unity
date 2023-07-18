/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.StatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.rest.authn.AuthenticationInterceptor;

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
	
	@BeforeEach
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
		
		HttpHost host = new HttpHost("https", "localhost", 53456);
		HttpClientContext localcontext = getClientContext(host);

		HttpGet get = new HttpGet("/mock/mock-rest/test/r1");

		ClassicHttpResponse		 response = client.executeOpen(host, get, localcontext);//){
		System.out.println(EntityUtils.toString(response.getEntity()));
		assertEquals(200, response.getCode(), new StatusLine(response).toString());
	}
	
	@Test
	public void requestNotAuthenticatedIsForbidden() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);
		HttpGet get = new HttpGet("/mock/mock-rest/test/r1");

		//no password, should fail.
		HttpResponse response = client.executeOpen(host, get, null);

		assertThat(response.getCode()).isEqualTo(Status.BAD_REQUEST.getStatusCode());
	}
	
	@Test
	public void exceptionIsMappedToHTTPError() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);
		HttpClientContext localcontext = getClientContext(host);
		
		HttpGet get = new HttpGet("/mock/mock-rest/test/r1/exception");
		ClassicHttpResponse response = client.executeOpen(host, get, localcontext);
		String entity = EntityUtils.toString(response.getEntity());
		System.out.println(entity);
		assertThat(response.getCode()).isEqualTo(Status.FORBIDDEN.getStatusCode());
		assertThat(response.getEntity().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(entity).containsSequence("Test exception");
	}
	
	@Test
	public void allowedCorsOriginIsAccepted() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);
		HttpClientContext localcontext = getClientContext(host);
		HttpOptions preflight = new HttpOptions("/mock/mock-rest/test/r1");
		preflight.addHeader("Origin", ALLOWED_ORIGIN2);
		preflight.addHeader("Access-Control-Request-Method", "PUT");
		
		ClassicHttpResponse response = client.executeOpen(host, preflight, localcontext);
		
		assertCorsAllowed(response);
	}

	@Test
	public void allowedCorsHeaderIsAccepted() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);
		HttpClientContext localcontext = getClientContext(host);
		HttpOptions preflight = new HttpOptions("/mock/mock-rest/test/r1");
		preflight.addHeader("Origin", ALLOWED_ORIGIN2);
		preflight.addHeader("Access-Control-Request-Method", "PUT");
		preflight.addHeader("Access-Control-Request-Headers", ALLOWED_HEADER);
		
		ClassicHttpResponse response = client.executeOpen(host, preflight, localcontext);
		
		assertCorsAllowed(response);
	}
	
	private void assertCorsAllowed(ClassicHttpResponse response)
	{
		assertEquals(200, response.getCode(), new StatusLine(response).toString());
		System.out.println(Arrays.toString(response.getHeaders()));
		assertThat(response.getHeaders("Access-Control-Allow-Origin")).isNotNull();
		assertThat(response.getHeaders("Access-Control-Allow-Origin").length).isEqualTo(1);
		assertThat(response.getHeaders("Access-Control-Allow-Origin")[0].getValue()).isEqualTo(ALLOWED_ORIGIN2);
		assertThat(response.getHeaders("Access-Control-Allow-Methods")).isNotNull();
		assertThat(response.getHeaders("Access-Control-Allow-Methods").length).isEqualTo(1);
		assertThat(response.getHeaders("Access-Control-Allow-Methods")[0].getValue()).contains("GET", "POST", "DELETE",
				"PUT");
	}
	
	@Test
	public void notAllowedCorsOriginIsNotAccepted() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);
		HttpClientContext localcontext = getClientContext(host);
		HttpOptions preflight = new HttpOptions("/mock/mock-rest/test/r1");
		preflight.addHeader("Origin", "http://notAllowedOrigin.com");
		
		HttpResponse response = client.executeOpen(host, preflight, localcontext);
		
		assertEquals(200, response.getCode(), new StatusLine(response).toString());
		System.out.println(Arrays.toString(response.getHeaders()));
		assertThat(response.getHeaders("Access-Control-Allow-Origin")).isNotNull();
		assertThat(response.getHeaders("Access-Control-Allow-Origin").length).isEqualTo(0);
		assertThat(response.getHeaders("Access-Control-Allow-Methods")).isNotNull();
		assertThat(response.getHeaders("Access-Control-Allow-Methods").length).isEqualTo(0);
	}
	
	@Test
	public void notAllowedCorsHeaderIsNotAccepted() throws Exception
	{
		HttpClient client = getClient();
		HttpHost host = new HttpHost("https", "localhost", 53456);
		HttpClientContext localcontext = getClientContext(host);
		HttpOptions preflight = new HttpOptions("/mock/mock-rest/test/r1");
		preflight.addHeader("Origin", ALLOWED_ORIGIN2);
		preflight.addHeader("Access-Control-Request-Method", "PUT");
		preflight.addHeader("Access-Control-Request-Headers", "X-notAllowed");
		
		HttpResponse response = client.executeOpen(host, preflight, localcontext);
		
		assertEquals(200, response.getCode(), new StatusLine(response).toString());
		System.out.println(Arrays.toString(response.getHeaders()));
		assertThat(response.getHeaders("Access-Control-Allow-Origin")).isNotNull();
		assertThat(response.getHeaders("Access-Control-Allow-Origin").length).isEqualTo(0);
		assertThat(response.getHeaders("Access-Control-Allow-Methods")).isNotNull();
		assertThat(response.getHeaders("Access-Control-Allow-Methods").length).isEqualTo(0);
	}
}
