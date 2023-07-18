/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.endpoint.Endpoint.EndpointState;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;
import pl.edu.icm.unity.engine.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.mock.MockEndpoint;

public class TestEndpoints extends DBIntegrationTestBase
{
	private static final String REALM_NAME = "testr";
	
	@Autowired
	private AuthenticatorManagement authnMan;
	@Autowired
	private AuthenticatorsRegistry authenticatorsReg;
	
	@BeforeEach
	public void addRealm() throws Exception
	{
		AuthenticationRealm realm = new AuthenticationRealm(REALM_NAME, "", 
				10, 10, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);
	}
	
	@Test 
	public void mockEndpointTypeIsReturned() throws Exception
	{
		List<EndpointTypeDescription> endpointTypes = endpointMan.getEndpointTypes();

		assertThat(endpointTypes).hasSize(1);
		EndpointTypeDescription type = endpointTypes.get(0);
		assertThat(type.getName()).isEqualTo(MockEndpoint.NAME);
	}
	
	@Test
	public void deployedEndpointIsReturned() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<String>(), "",
				REALM_NAME);
		endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg);
		
		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		
		assertThat(endpoints).hasSize(1);
		assertThat(endpoints.get(0).getEndpoint().getConfiguration()).isEqualTo(cfg);
		assertThat(endpoints.get(0).getRealm().getName()).isEqualTo(REALM_NAME);
		assertThat(endpoints.get(0).getEndpoint().getName()).isEqualTo("endpoint1");
		assertThat(endpoints.get(0).getEndpoint().getContextAddress()).isEqualTo("/foo");
	}
	
	@Test
	public void updatedEndpointIsReturned() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<String>(), "",
				REALM_NAME);
		endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg);
		
		EndpointConfiguration cfg2 = new EndpointConfiguration(
				new I18nString("endpoint1I"), "ada", new ArrayList<>(), "", REALM_NAME);
		endpointMan.updateEndpoint("endpoint1", cfg2);

		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		assertThat(endpoints).hasSize(1);
		assertThat(endpoints.get(0).getEndpoint().getConfiguration()).isEqualTo(cfg2);
		assertThat(endpoints.get(0).getRealm().getName()).isEqualTo(REALM_NAME);
		assertThat(endpoints.get(0).getEndpoint().getName()).isEqualTo("endpoint1");
		assertThat(endpoints.get(0).getEndpoint().getContextAddress()).isEqualTo("/foo");
	}
	
	@Test
	public void removedEndpointIsNotReturned() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<String>(), "",
				REALM_NAME);
		endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg);
		
		endpointMan.undeploy("endpoint1");
		
		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		
		assertThat(endpoints).isEmpty();
	}
	
	@Test
	public void duplicatedEndpointIsNotDeployed() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<String>(), "",
				REALM_NAME);
		endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg);
		
		try
		{
			endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg);
			fail("Should get an exception");
		} catch (EngineException e)
		{
			assertThat(e.getMessage()).containsSequence("exists");
		}
		
		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		assertThat(endpoints).hasSize(1);
	}

	@Test
	public void endpointWithWrongConfigurationIsNotDeployed() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<String>(), 
				MockEndpoint.WRONG_CONFIG, REALM_NAME);
		try
		{
			endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg);
			fail("Should get an exception");
		} catch (EngineException e)
		{
			assertThat(e.getMessage()).containsSequence("configuration");
		}
		
		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		assertThat(endpoints).isEmpty();
	}
	
	@Test
	public void endpointWithWrongPathIsNotDeployed() throws Exception
	{
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<String>(), 
				"", REALM_NAME);
		try
		{
			endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "MISSING_LEADING_SLASH", cfg);
			fail("Should get an exception");
		} catch (EngineException e)
		{
			assertThat(e.getMessage()).containsSequence("path");
		}
		try
		{
			endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/two/slashes", cfg);
			fail("Should get an exception");
		} catch (EngineException e)
		{
			assertThat(e.getMessage()).containsSequence("path");
		}
		try
		{
			endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/illegal?query", cfg);
			fail("Should get an exception");
		} catch (EngineException e)
		{
			assertThat(e.getMessage()).containsSequence("path");
		}

		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		assertThat(endpoints).isEmpty();
	}
	
	@Test
	public void testInitializationOfEndpointsAtStartup() throws Exception
	{
		//test initial loading from DB: create, remove from the server, load
		EndpointConfiguration cfg2 = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<String>(), "", REALM_NAME);
		endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg2);
		EndpointConfiguration cfg3 = new EndpointConfiguration(new I18nString("endpoint2"), 
				"desc", new ArrayList<String>(), "", REALM_NAME);
		endpointMan.deploy(MockEndpoint.NAME, "endpoint2", "/foo2", cfg3);
		List<ResolvedEndpoint> endpoints = endpointMan.getDeployedEndpoints();
		assertThat(endpoints).hasSize(2);
		endpointMan.updateEndpoint(endpoints.get(0).getEndpoint().getName(), 
				new EndpointConfiguration(new I18nString("endp1"), 
				"endp1", null, null, REALM_NAME));
		endpointMan.updateEndpoint(endpoints.get(1).getEndpoint().getName(), 
				new EndpointConfiguration(new I18nString("endp2"),
				"endp2", null, null, REALM_NAME));

		internalEndpointMan.undeploy(endpoints.get(0).getEndpoint().getName());
		internalEndpointMan.undeploy(endpoints.get(1).getEndpoint().getName());

		endpoints = endpointMan.getDeployedEndpoints();
		assertThat(endpoints).isEmpty();
		
		internalEndpointMan.loadPersistedEndpoints();
		
		endpoints = endpointMan.getDeployedEndpoints();
		assertThat(endpoints).hasSize(2);
		ResolvedEndpoint re1 = endpoints.stream().filter(
				re -> re.getEndpoint().getConfiguration().getDescription().
				equals("endp1")).
				findAny().get();
		ResolvedEndpoint re2 = endpoints.stream().filter(
				re -> re.getEndpoint().getConfiguration().getDescription().
				equals("endp2")).
				findAny().get();
		assertThat(re1.getEndpoint().getConfiguration().getDisplayedName().getDefaultValue()).
				isEqualTo("endp1");
		assertThat(re2.getEndpoint().getConfiguration().getDisplayedName().getDefaultValue()).
				isEqualTo("endp2");
		
		//finally test if removal from DB works
		internalEndpointMan.removeAllPersistedEndpoints();
		internalEndpointMan.loadPersistedEndpoints();
		endpoints = endpointMan.getDeployedEndpoints();
		assertThat(endpoints).isEmpty();
	}
	
	@Test
	public void shouldInitializeEndpointWithAutheticators() throws Exception
	{
		super.setupMockAuthn();
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg.getAuthenticatorTypes();
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		
		authnMan.createAuthenticator("auth1", authType.getVerificationMethod(), "config", "credential1");
		
		authnMan.createAuthenticator("auth2", "mockpassword", "config", CRED_MOCK);
		
		
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", Arrays.asList("auth1", "auth2"), "", REALM_NAME);
		
		endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/xxx", cfg);
		List<EndpointInstance> deployedEndpoints = internalEndpointMan.getDeployedEndpoints();
	
		assertThat(deployedEndpoints).hasSize(1);
		assertThat(deployedEndpoints.get(0).getAuthenticationFlows().get(0).getId() ).isEqualTo("auth1");
		assertThat(deployedEndpoints.get(0).getAuthenticationFlows().get(1).getId() ).isEqualTo("auth2");
	}
	
	@Test
	public void shouldUndeployEndpoint() throws Exception
	{
		EndpointConfiguration cfg2 = new EndpointConfiguration(new I18nString("endpoint1"), 
				"desc", new ArrayList<String>(), "", REALM_NAME);
		endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg2);
		endpointMan.undeploy("endpoint1");	
		List<Endpoint> endpoints = endpointMan.getEndpoints();
		assertThat(endpoints.size() ).isEqualTo(1);	
		Endpoint endpoint = endpoints.get(0);
		assertThat(endpoint.getState() ).isEqualTo(EndpointState.UNDEPLOYED);	
	}	
}
