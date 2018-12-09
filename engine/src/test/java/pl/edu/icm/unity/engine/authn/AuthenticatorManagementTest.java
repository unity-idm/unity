/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.endpoint.InternalEndpointManagement;
import pl.edu.icm.unity.engine.mock.MockEndpoint;
import pl.edu.icm.unity.engine.mock.MockPasswordVerificatorFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorInfo;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

public class AuthenticatorManagementTest extends DBIntegrationTestBase
{
	@Autowired
	private AuthenticatorManagement authnMan;
	
	@Autowired
	private AuthenticationFlowManagement authnFlowMan;
	
	@Autowired
	private InternalEndpointManagement internalEndpointMan;
	
	@Autowired
	private AuthenticatorsRegistry authenticatorsReg;
	
	@Test
	public void shouldFailAuthenticationWithIncorrectPassword() throws Exception
	{
		//create credential requirement and an identity with it 
		super.setupMockAuthn();
		createRealmEndpointAndAuthenticator();
		
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "CN=foo"), 
				"crMock", EntityState.valid, false);

		EntityParam entityP = new EntityParam(id);
		eCredMan.setEntityCredential(entityP, "credential1", "wrongpassword");
		
		MockEndpoint endpoint = (MockEndpoint) internalEndpointMan.getDeployedEndpoints().iterator().next();
		Throwable error = Assertions.catchThrowable(() -> endpoint.authenticate());
		
		Assertions.assertThat(error).isInstanceOf(IllegalCredentialException.class);
	}

	@Test
	public void shouldAuthenticateWithCorrectPassword() throws Exception
	{
		//create credential requirement and an identity with it 
		super.setupMockAuthn();		
		createRealmEndpointAndAuthenticator();

		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "CN=foo"), 
				"crMock", EntityState.valid, false);
		EntityParam entityP = new EntityParam(id);
		MockEndpoint endpoint = (MockEndpoint) internalEndpointMan.getDeployedEndpoints().iterator().next();
		eCredMan.setEntityCredential(entityP, "credential1", "bar");
		
		long entityId = endpoint.authenticate();
		Entity entity = idsMan.getEntity(entityP);
		assertEquals(entityId, entity.getId().longValue());
	}

	private void createRealmEndpointAndAuthenticator() throws Exception
	{
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg.getAuthenticatorTypesByBinding("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth1", authType.getVerificationMethod(), "bbb", "credential1");
		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("auth1")));
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 10, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"),
				"desc", Collections.singletonList("flow1"), "", realm.getName());
		endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg);
	}
	
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionWhenAddAuthFlowWithMissingAuthenticator()
			throws EngineException
	{
		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("wrong")));
	}
	
	
	@Test
	public void shouldReturnAllAuthnTypes() throws Exception
	{
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg.getAuthenticatorTypesByBinding("web");
		assertEquals(1, authTypes.size());
		authTypes = authenticatorsReg.getAuthenticatorTypes();
		assertEquals(1, authTypes.size());
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		assertEquals(true, authType.isLocal());
		assertEquals(MockPasswordVerificatorFactory.ID, authType.getVerificationMethod());
	}
	
	@Test
	public void shouldReturnCreatedAuthenticator() throws Exception
	{	
		super.setupMockAuthn();
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg.getAuthenticatorTypesByBinding("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		
		authnMan.createAuthenticator("auth0", authType.getVerificationMethod(), "CONFIG", "credential1");

		Collection<AuthenticatorInfo> auths = authnMan.getAuthenticators("web");
		assertEquals(1, auths.size());
		AuthenticatorInfo authInstanceR = auths.iterator().next();
		assertThat(authInstanceR.getId(), is("auth0"));
		assertThat(authInstanceR.getTypeDescription(), is(authType));
		assertThat(authInstanceR.getConfiguration(), is("CONFIG"));
		assertThat(authInstanceR.getLocalCredentialName().get(), is("credential1"));
		assertThat(authInstanceR.getSupportedBindings(), is(Sets.newHashSet("web", "web2")));
	}

	@Test
	public void shouldReturnUpdatedAuthenticator() throws Exception
	{	
		super.setupMockAuthn();
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg.getAuthenticatorTypesByBinding("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth1", authType.getVerificationMethod(), "bbb", "credential1");

		authnMan.updateAuthenticator("auth1", "UPDATED", "credential1");

		Collection<AuthenticatorInfo> auths = authnMan.getAuthenticators("web");
		assertEquals(1, auths.size());
		AuthenticatorInfo authInstanceR = auths.iterator().next();
		assertThat(authInstanceR.getId(), is("auth1"));
		assertThat(authInstanceR.getTypeDescription(), is(authType));
		assertThat(authInstanceR.getConfiguration(), is("UPDATED"));
		assertThat(authInstanceR.getLocalCredentialName().get(), is("credential1"));
		assertThat(authInstanceR.getSupportedBindings(), is(Sets.newHashSet("web", "web2")));
	}
	
	@Test
	public void shouldReturnAddedFlow() throws Exception
	{
		super.setupMockAuthn();
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg.getAuthenticatorTypesByBinding("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth0", authType.getVerificationMethod(), "aaa", "credential1");

		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("auth0")));

		Collection<AuthenticationFlowDefinition> authFlows = authnFlowMan.getAuthenticationFlows();
		assertThat(authFlows.size(), is(1));
		AuthenticationFlowDefinition flow = authFlows.iterator().next();
		assertThat(flow.getFirstFactorAuthenticators(), is(Sets.newHashSet("auth0")));
		assertThat(flow.getPolicy(), is(Policy.NEVER));
	}
	
	@Test
	public void shouldReturnUpdatedFlow() throws Exception
	{
		super.setupMockAuthn();
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg.getAuthenticatorTypesByBinding("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth0", authType.getVerificationMethod(), "aaa", "credential1");
		authnMan.createAuthenticator("auth1", authType.getVerificationMethod(), "bbb", "credential1");
		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("auth0")));
		
		authnFlowMan.updateAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.REQUIRE, Sets.newHashSet("auth1")));
		
		Collection<AuthenticationFlowDefinition> authFlows = authnFlowMan.getAuthenticationFlows();
		assertThat(authFlows.size(), is(1));
		AuthenticationFlowDefinition flow = authFlows.iterator().next();
		assertThat(flow.getFirstFactorAuthenticators(), is(Sets.newHashSet("auth1")));
		assertThat(flow.getPolicy(), is(Policy.REQUIRE));
	}
	
	
	@Test
	public void shouldNotRemoveUsedAutheticatorOrFlow() throws Exception
	{
		super.setupMockAuthn();
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 10, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);
		
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg.getAuthenticatorTypesByBinding("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		
		AuthenticatorInfo authInstance1 = authnMan.createAuthenticator(
				"auth0", authType.getVerificationMethod(), "bbb", "credential1");

		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("auth0")));
		
		
		List<EndpointTypeDescription> endpointTypes = endpointMan.getEndpointTypes();
		assertEquals(1, endpointTypes.size());
		EndpointTypeDescription type = endpointTypes.get(0);
		
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"),
				"desc", new ArrayList<String>(), "", realm.getName());
		endpointMan.deploy(type.getName(), "endpoint1", "/foo", cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		endpointMan.updateEndpoint(endpoints.get(0).getEndpoint().getName(), 
				new EndpointConfiguration(new I18nString("ada"), 
				"ada", Collections.singletonList("flow1"), "", realm.getName()));

		List<String> endpointFlows = endpointMan.getEndpoints().get(0).
				getEndpoint().getConfiguration().getAuthenticationOptions();
		assertThat(endpointFlows.size(), is(1));
		
		//remove a used authenticator
		try
		{
			authnMan.removeAuthenticator(authInstance1.getId());
			fail("Was able to remove a used authenticator");
		} catch (IllegalArgumentException e) {}
		
		
		//remove a used authentication flow
		try
		{
			authnFlowMan.removeAuthenticationFlow("flow1");
			fail("Was able to remove a used authentication flow");
		} catch (IllegalArgumentException e)
		{
		}
		
		
		endpointMan.updateEndpoint(endpoints.get(0).getEndpoint().getName(), 
				new EndpointConfiguration(new I18nString("ada"), "ada", 
						new ArrayList<String>(), "", realm.getName()));

		authnFlowMan.removeAuthenticationFlow("flow1");
		Collection<AuthenticationFlowDefinition> authFlows = authnFlowMan.getAuthenticationFlows();
		assertThat(authFlows.size(), is(0));

		authnMan.removeAuthenticator(authInstance1.getId());
		Collection<AuthenticatorInfo> auths = authnMan.getAuthenticators(null);
		assertThat(auths.size(), is(0));	
	}
}
