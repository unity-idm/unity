/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.junit.Before;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pl.edu.icm.unity.rest.TestRESTBase;

/**
 *
 * @author Krzysztof Benedyczak
 */
public abstract class RESTAdminTestBase extends TestRESTBase
{
	protected HttpClientContext localcontext;
	
	protected ObjectMapper m = new ObjectMapper().findAndRegisterModules();

	protected HttpHost host;

	protected HttpClient client;
	
	{
		m.enable(SerializationFeature.INDENT_OUTPUT);
	}

	@Before
	public void setup() throws Exception
	{
		setupPasswordAuthn();
		createUsernameUserWithRole("System Manager");
		super.deployEndpoint(RESTAdminEndpoint.NAME, 
				"restAdmin", "/restadm");		
		client = getClient();
		host = getHost();
		localcontext = getClientContext(host);
	}

	protected HttpHost getHost() {
		return new HttpHost("https", "localhost", 53456);
	}
	
}
