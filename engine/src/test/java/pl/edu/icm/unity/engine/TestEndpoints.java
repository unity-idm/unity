/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import pl.edu.icm.unity.engine.mock.MockEndpoint;
import pl.edu.icm.unity.engine.mock.MockEndpointFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

public class TestEndpoints extends DBIntegrationTestBase
{
	private static final String REALM_NAME = "testr";
	
	@Before
	public void addRealm() throws Exception
	{
		AuthenticationRealm realm = new AuthenticationRealm(REALM_NAME, "", 
				10, 10, -1, 600);
		realmsMan.addRealm(realm);
	}
	
	@Test 
	public void mockEndpointTypeIsReturned() throws Exception
	{
		List<EndpointTypeDescription> endpointTypes = endpointMan.getEndpointTypes();

		assertThat(endpointTypes.size(), is(1));
		EndpointTypeDescription type = endpointTypes.get(0);
		assertThat(type.getName(), is(MockEndpointFactory.NAME));
	}
	
	@Test
	public void deployedEndpointIsReturned() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<AuthenticationOptionDescription>(), "",
				REALM_NAME);
		endpointMan.deploy(MockEndpointFactory.NAME, "endpoint1", "/foo", cfg);
		
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		
		assertThat(endpoints.size(), is(1));
		assertThat(endpoints.get(0).getAuthenticatorSets().isEmpty(), is(true));
		assertThat(endpoints.get(0).getRealm().getName(), is(REALM_NAME));
		assertThat(endpoints.get(0).getDescription(), is("desc"));
		assertThat(endpoints.get(0).getId(), is("endpoint1"));
		assertThat(endpoints.get(0).getContextAddress(), is("/foo"));
		assertThat(endpoints.get(0).getDisplayedName(), is(new I18nString("endpoint1")));
	}
	
	@Test
	public void updatedEndpointIsReturned() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<AuthenticationOptionDescription>(), "",
				REALM_NAME);
		endpointMan.deploy(MockEndpointFactory.NAME, "endpoint1", "/foo", cfg);
		
		endpointMan.updateEndpoint("endpoint1", new EndpointConfiguration(
				new I18nString("endpoint1I"), "ada", null, null, REALM_NAME));

		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertThat(endpoints.size(), is(1));
		assertThat(endpoints.get(0).getAuthenticatorSets().isEmpty(), is(true));
		assertThat(endpoints.get(0).getRealm().getName(), is(REALM_NAME));
		assertThat(endpoints.get(0).getDescription(), is("ada"));
		assertThat(endpoints.get(0).getId(), is("endpoint1"));
		assertThat(endpoints.get(0).getContextAddress(), is("/foo"));
		assertThat(endpoints.get(0).getDisplayedName(), is(new I18nString("endpoint1I")));
	}
	
	@Test
	public void removedEndpointIsNotReturned() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<AuthenticationOptionDescription>(), "",
				REALM_NAME);
		endpointMan.deploy(MockEndpointFactory.NAME, "endpoint1", "/foo", cfg);
		
		endpointMan.undeploy("endpoint1");
		
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		
		assertThat(endpoints.isEmpty(), is(true));
	}
	
	@Test
	public void duplicatedEndpointIsNotDeployed() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<AuthenticationOptionDescription>(), "",
				REALM_NAME);
		endpointMan.deploy(MockEndpointFactory.NAME, "endpoint1", "/foo", cfg);
		
		try
		{
			endpointMan.deploy(MockEndpointFactory.NAME, "endpoint1", "/foo", cfg);
			fail("Should get an exception");
		} catch (EngineException e)
		{
			assertThat(e.getMessage(), containsString("exists"));
		}
		
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertThat(endpoints.size(), is(1));
	}

	@Test
	public void endpointWithWrongConfigurationIsNotDeployed() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<AuthenticationOptionDescription>(), 
				MockEndpoint.WRONG_CONFIG, REALM_NAME);
		try
		{
			endpointMan.deploy(MockEndpointFactory.NAME, "endpoint1", "/foo", cfg);
			fail("Should get an exception");
		} catch (EngineException e)
		{
			assertThat(e.getMessage(), containsString("configuration"));
		}
		
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertThat(endpoints.isEmpty(), is(true));
	}
	
	@Test
	public void testInitializationOfEndpointsAtStartup() throws Exception
	{
		//test initial loading from DB: create, remove from the server, load
		EndpointConfiguration cfg2 = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<AuthenticationOptionDescription>(), "", REALM_NAME);
		endpointMan.deploy(MockEndpointFactory.NAME, "endpoint1", "/foo", cfg2);
		EndpointConfiguration cfg3 = new EndpointConfiguration(new I18nString("endpoint2"), 
				"desc", new ArrayList<AuthenticationOptionDescription>(), "", REALM_NAME);
		endpointMan.deploy(MockEndpointFactory.NAME, "endpoint2", "/foo2", cfg3);
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(2, endpoints.size());
		endpointMan.updateEndpoint(endpoints.get(0).getId(), new EndpointConfiguration(new I18nString("endp1"), 
				"endp1", null, null, REALM_NAME));
		endpointMan.updateEndpoint(endpoints.get(1).getId(), new EndpointConfiguration(new I18nString("endp2"),
				"endp2", null, null, REALM_NAME));

		internalEndpointMan.undeploy(endpoints.get(0).getId());
		internalEndpointMan.undeploy(endpoints.get(1).getId());

		endpoints = endpointMan.getEndpoints();
		assertEquals(0, endpoints.size());
		
		internalEndpointMan.loadPersistedEndpoints();
		
		endpoints = endpointMan.getEndpoints();
		assertEquals(2, endpoints.size());
		assertEquals("endp1", endpoints.get(0).getDescription());
		assertEquals("endp2", endpoints.get(1).getDescription());
		assertEquals("endp1", endpoints.get(0).getDisplayedName().getDefaultValue());
		assertEquals("endp2", endpoints.get(1).getDisplayedName().getDefaultValue());
		
		//finally test if removal from DB works
		internalEndpointMan.removeAllPersistedEndpoints();
		internalEndpointMan.loadPersistedEndpoints();
		endpoints = endpointMan.getEndpoints();
		assertEquals(0, endpoints.size());
	}
}
