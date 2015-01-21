/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.performance;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;

import pl.edu.icm.unity.rest.MockRESTEndpointFactory;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
/**
 * Test user login performance
 * 
 * @author P.Piernik
 * 
 */
public class TstPerfLogin extends PerformanceTestBase
{
	
	public final int USERS = 1000; 
	public final int WARM_SIZE = 10;
	
	@Test
	public void testLogin() throws Exception
	{
		
		
		addUsers(WARM_SIZE + USERS);

		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 10, 100, -1, 600);
		realmsMan.addRealm(realm);

		List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
		authnCfg.add(new AuthenticatorSet(Collections.singleton(AUTHENTICATOR_REST_PASS)));
		endpointMan.deploy(MockRESTEndpointFactory.NAME, "endpoint1", new I18nString("endpoint1"), "/mock", "desc",
				authnCfg, "", realm.getName());
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());
		httpServer.start();
		HttpHost host = new HttpHost("localhost", 53456, "https");

		// warn-up ...login user
		for (int i = 0; i < WARM_SIZE; i++)
		{
			HttpClient client = getClient();
			HttpContext localcontext = getClientContext(client, host, "user" + i,
					"PassWord8743#%$^&*");
			HttpGet get = new HttpGet("/mock/mock-rest/test/r1");
			HttpResponse response = client.execute(host, get, localcontext);
			assertEquals(response.getStatusLine().toString(), 200, response
					.getStatusLine().getStatusCode());
		}

		int jump = USERS / TEST_REPETITIONS;
		int index = WARM_SIZE;
		for (int j = 0; j < TEST_REPETITIONS; j++)
		{

			timer.startTimer();
			for (int i = 0; i < jump; i++)
			{
				
				HttpClient client = getClient();
				HttpContext localcontext = getClientContext(client, host,
						"user" + index, "PassWord8743#%$^&*");
				index++;
				HttpGet get = new HttpGet("/mock/mock-rest/test/r1");
				HttpResponse response = client.execute(host, get, localcontext);
				assertEquals(response.getStatusLine().toString(), 200, response
						.getStatusLine().getStatusCode());
			}
			timer.stopTimer(jump, "Login user");
		}
		timer.calculateResults("Login user");

	}
}
