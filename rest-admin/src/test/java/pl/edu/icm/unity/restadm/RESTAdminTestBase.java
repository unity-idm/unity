/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.protocol.HttpContext;
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
	protected HttpContext localcontext;
	
	protected ObjectMapper m = new ObjectMapper();

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
		host = new HttpHost("localhost", 53456, "https");
		localcontext = getClientContext(host);
	}

}
