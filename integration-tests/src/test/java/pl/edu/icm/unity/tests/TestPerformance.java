package pl.edu.icm.unity.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.rest.MockRESTEndpointFactory;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

public class TestPerformance extends IntegrationTestBase
{	int T = 3; //Group tiers
	int N = 10; // Group in tier
	int NU = 100; // users 

	int ImageAttr = 10; // image attribute. BIG attr
	int StingAttr = 100; // string attribute
	int FloatAttr = 100; // float attributes
	int IntAttr = 100; // int attributes
	
	@Test
	public void testGetEntities() throws EngineException, IOException
	{
		addGroups(N,T);
		addUsers(NU);
		moveUserToGroup(NU, N, T);
		
		//warn-up
		getAllEntities(NU/10);
			
		
		for (int i = 0; i < 10; i++)
		{
			timer.startTimer();
			getAllEntities(N);
			timer.stopTimer(NU, "Get entity");
		}
		timer.calculateResults("Get entity");
	}
	@Test
	public void testGetAttributes() throws EngineException, IOException
	{
		addGroups(N,T);
		addUsers(NU);
		ArrayList<String> enInGroup = moveUserToGroup(NU, N, T);
		ArrayList<Entity> entities = getAllEntities(NU);
		
		addAttributeTypes(N);
		Map<String, AttributeType> attributeTypesAsMap = attrsMan.getAttributeTypesAsMap();
		
		addRandomAttributeToEntities(entities, enInGroup, attributeTypesAsMap, ImageAttr,
				StingAttr, IntAttr, FloatAttr);
		
		//warn-up
		getUsersAttr(NU/10, enInGroup, false);
		
		for (int i = 0; i < 10; i++)
		{
			timer.startTimer();
			getUsersAttr(N, enInGroup, false);
			timer.stopTimer(N, "Get attribute for user");
		}
		timer.calculateResults("Get attribute for user");
	}
	
	@Test
	public void testGetAttributesWithStatment() throws EngineException, IOException
	{
		addGroups(N,T);
		addUsers(NU);
		ArrayList<String> enInGroup = moveUserToGroup(NU, N, T);
		ArrayList<Entity> entities = getAllEntities(NU);
		
		addAttributeTypes(N);
		Map<String, AttributeType> attributeTypesAsMap = attrsMan.getAttributeTypesAsMap();
		
		addRandomAttributeToEntities(entities, enInGroup, attributeTypesAsMap, ImageAttr,
				StingAttr, IntAttr, FloatAttr);
		
		ArrayList<GroupContents> con = getGroupContent(new Group("/"));
		
		
		addAttributeTypeForStatments();
		attributeTypesAsMap = attrsMan.getAttributeTypesAsMap();
		addAttrStatments(con, attributeTypesAsMap, T);
		
		//warn-up
		getUsersAttr(N/10, enInGroup, false);
		
		for (int i = 0; i < 10; i++)
		{
			timer.startTimer();
			getUsersAttr(N, enInGroup, false);
			timer.stopTimer(N, "Get attribute for user with eval attr statment");
		}
		timer.calculateResults("Get attribute for user with eval attr statment");
		
	}	
	
	@Test
	public void testLogin() throws Exception
	{
		int k = 10;
		addUsers(k * NU);

		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 10, 100, -1, 600);
		realmsMan.addRealm(realm);

		List<AuthenticatorSet> authnCfg = new ArrayList<AuthenticatorSet>();
		authnCfg.add(new AuthenticatorSet(Collections.singleton(AUTHENTICATOR_REST_PASS)));
		endpointMan.deploy(MockRESTEndpointFactory.NAME, "endpoint1", "/mock", "desc",
				authnCfg, "", realm.getName());
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());
		httpServer.start();
		HttpHost host = new HttpHost("localhost", 53456, "https");

		// warn-up ...login user
		for (int i = 0; i < k / 10 * NU; i++)
		{
			DefaultHttpClient client = getClient();
			BasicHttpContext localcontext = getClientContext(client, host, "user" + i,
					"PassWord8743#%$^&*");
			HttpGet get = new HttpGet("/mock/mock-rest/test/r1");
			HttpResponse response = client.execute(host, get, localcontext);
			assertEquals(response.getStatusLine().toString(), 200, response
					.getStatusLine().getStatusCode());
		}

		for (int j = 0; j < k - 1; j++)
		{

			timer.startTimer();
			for (int i = j * NU; i < (j + 1) * NU; i++)
			{
				DefaultHttpClient client = getClient();
				BasicHttpContext localcontext = getClientContext(client, host,
						"user" + i, "PassWord8743#%$^&*");
				HttpGet get = new HttpGet("/mock/mock-rest/test/r1");
				HttpResponse response = client.execute(host, get, localcontext);
				assertEquals(response.getStatusLine().toString(), 200, response
						.getStatusLine().getStatusCode());
			}
			timer.stopTimer(NU, "Login user");
		}
		timer.calculateResults("Login user");

	}
	
	
}

