/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pl.edu.icm.unity.rest.TestRESTBase;

/**
 *
 * @author Krzysztof Benedyczak
 */
abstract class RESTAdminTestBase extends TestRESTBase
{
	
	protected ObjectMapper m = new ObjectMapper().findAndRegisterModules();

	protected HttpHost host;

	protected HttpClient client;

	protected int port;
	
	{
		m.enable(SerializationFeature.INDENT_OUTPUT);
	}

	@BeforeEach
	public void setup() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUserWithRole("System Manager");
		super.deployEndpoint(RESTAdminEndpoint.NAME, 
				"restAdmin", "/restadm");
		port = httpServer.getUrls()[0].getPort();
		client = getClient(port);
		host = getHost();
	}

	protected HttpHost getHost() {
		return new HttpHost("https", "localhost", port);
	}

	protected String executeQuery(HttpUriRequestBase request) throws Exception
	{
		return executeQuery(request, getClientContext(host));
	}

	protected String executeQuery(HttpUriRequestBase request, HttpContext context) throws Exception
	{
		HttpClient client = getClient(port);
		return client.execute(host, request, context, new BasicHttpClientResponseHandler());
	}
}
