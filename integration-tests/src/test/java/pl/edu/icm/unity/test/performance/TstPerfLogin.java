/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.performance;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.rest.MockRESTEndpoint;
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
	
	//@Test
	public void testLogin() throws Exception
	{
		
		
		addUsers(WARM_SIZE + USERS);

		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 10, 100, RememberMePolicy.disallow ,1, 600);
		realmsMan.addRealm(realm);
		authFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				AUTHENTICATION_FLOW_PASS, Policy.NEVER,
				Sets.newHashSet(AUTHENTICATOR_REST_PASS)));
		
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), "desc",
				Lists.newArrayList(AUTHENTICATION_FLOW_PASS), "", realm.getName());
		endpointMan.deploy(MockRESTEndpoint.NAME, "endpoint1", "/mock", cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		assertEquals(1, endpoints.size());
		httpServer.start();
		int port = httpServer.getUrls()[0].getPort();
		HttpHost host = new HttpHost("https", "localhost", port);

		// warn-up ...login user
		for (int i = 0; i < WARM_SIZE; i++)
		{
			HttpClient client = getClient(port);
			HttpClientContext localcontext = getClientContext(host, "user" + i,
					"PassWord8743#%$^&*");
			HttpGet get = new HttpGet("/mock/mock-rest/test/r1");
			client.execute(host, get, localcontext, new BasicHttpClientResponseHandler());
		}

		int jump = USERS / TEST_REPETITIONS;
		int index = WARM_SIZE;
		for (int j = 0; j < TEST_REPETITIONS; j++)
		{

			timer.startTimer();
			for (int i = 0; i < jump; i++)
			{
				
				HttpClient client = getClient(port);
				HttpClientContext localcontext = getClientContext(host,
						"user" + index, "PassWord8743#%$^&*");
				index++;
				HttpGet get = new HttpGet("/mock/mock-rest/test/r1");
				client.execute(host, get, localcontext, new BasicHttpClientResponseHandler());
			}
			timer.stopTimer(jump, "Login user");
		}
		timer.calculateResults("Login user");

	}
}
