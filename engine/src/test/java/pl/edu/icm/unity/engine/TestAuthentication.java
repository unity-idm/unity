/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.authz.AuthorizationManagerImpl;
import pl.edu.icm.unity.engine.endpoint.InternalEndpointManagement;
import pl.edu.icm.unity.engine.mock.MockEndpoint;
import pl.edu.icm.unity.engine.mock.MockPasswordVerificatorFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;

public class TestAuthentication extends DBIntegrationTestBase
{
	@Autowired
	private AuthenticatorManagement authnMan;
	
	@Autowired
	private AuthenticationFlowManagement authnFlowMan;
	
	@Autowired
	private InternalEndpointManagement internalEndpointMan;
	
	@Test
	public void testAuthentication() throws Exception
	{
		//create credential requirement and an identity with it 
		super.setupMockAuthn();
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 10, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);
		
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "CN=foo"), 
				"crMock", EntityState.valid, false);

		//create authenticator, authentication flow and an endpoint with it
		Collection<AuthenticatorTypeDescription> authTypes = authnMan.getAuthenticatorTypes("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth1", authType.getId(), "6", "bbb", "credential1");
		
		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("auth1")));

		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"),
				"desc", Collections.singletonList("flow1"), "", realm.getName());
		endpointMan.deploy(MockEndpoint.NAME, "endpoint1", "/foo", cfg);

		//set wrong password 
		EntityParam entityP = new EntityParam(id);
		eCredMan.setEntityCredential(entityP, "credential1", "password");
		
		MockEndpoint endpoint = (MockEndpoint) internalEndpointMan.getDeployedEndpoints().iterator().next();
		try
		{
			endpoint.authenticate();
			fail("Authn with wrong cred succeeded");
		} catch (IllegalCredentialException e) {}
		
		//set correct password 
		eCredMan.setEntityCredential(entityP, "credential1", "bar");
		long entityId = endpoint.authenticate();
		Entity entity = idsMan.getEntity(entityP);
		assertEquals(entityId, entity.getId().longValue());
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionWhenAddAuthFlowWithWrongAuthenticator()
			throws EngineException
	{
		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("wrong")));
	}
	
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowExceptionWhenAddAuthFlowWithDiffrentBindingAuthenticator()
			throws Exception
	{
		super.setupMockAuthn();
		Collection<AuthenticatorTypeDescription> authenticatorTypes1 = authnMan.getAuthenticatorTypes("web");
		Collection<AuthenticatorTypeDescription> authenticatorTypes2 = authnMan.getAuthenticatorTypes("web2");
		
		authnMan.createAuthenticator(
				"auth0", authenticatorTypes1.iterator().next().getId(), "8", "aaa", "credential1");
		
		authnMan.createAuthenticator(
				"auth1", authenticatorTypes2.iterator().next().getId(), "8", "bbb", "credential1");
		
		
		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("auth0","auth1")));
	}

	@Test
	public void shouldReturnAllAuthnTypes() throws Exception
	{
		Collection<AuthenticatorTypeDescription> authTypes = authnMan.getAuthenticatorTypes("web");
		assertEquals(1, authTypes.size());
		authTypes = authnMan.getAuthenticatorTypes(null);
		assertEquals(2, authTypes.size());
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		assertEquals(true, authType.isLocal());
		assertEquals("mockretrieval", authType.getRetrievalMethod());
		assertEquals(MockPasswordVerificatorFactory.ID, authType.getVerificationMethod());
		assertEquals("web", authType.getSupportedBinding());

	}
	
	@Test
	public void authenticatorCRUDTest() throws Exception
	{	
		super.setupMockAuthn();
		Collection<AuthenticatorTypeDescription> authTypes = authnMan
				.getAuthenticatorTypes("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		
		authnMan.createAuthenticator("auth0",
				authType.getId(), "8", "aaa", "credential1");

		authnMan.createAuthenticator("auth1",
				authType.getId(), "8", "bbb", "credential1");

		Collection<AuthenticatorInstance> auths = authnMan.getAuthenticators("web");
		assertEquals(2, auths.size());
		AuthenticatorInstance authInstanceR = auths.iterator().next();
		assertEquals("auth0", authInstanceR.getId());
		assertEquals("aaa", authInstanceR.getRetrievalConfiguration());
		assertNull(authInstanceR.getVerificatorConfiguration());

		authnMan.updateAuthenticator("auth1", "9", "b", "credential1");

		auths = authnMan.getAuthenticators("web");
		assertEquals(2, auths.size());
		Iterator<AuthenticatorInstance> iterator = auths.iterator();
		iterator.next();
		authInstanceR = iterator.next();
		assertEquals("auth1", authInstanceR.getId());
		assertEquals("b", authInstanceR.getRetrievalConfiguration());
		assertNull(authInstanceR.getVerificatorConfiguration());
	}
	
	@Test
	public void authenticationFlowCRUDTest() throws Exception
	{
		super.setupMockAuthn();
		Collection<AuthenticatorTypeDescription> authTypes = authnMan
				.getAuthenticatorTypes("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth0",
				authType.getId(), "8", "aaa", "credential1");

		authnMan.createAuthenticator("auth1",
				authType.getId(), "8", "bbb", "credential1");
		
		authnFlowMan.addAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("auth0")));
		Collection<AuthenticationFlowDefinition> authFlows = authnFlowMan.getAuthenticationFlows();
		assertThat(authFlows.size(), is(1));
		assertThat(authFlows.iterator().next().getFirstFactorAuthenticators().iterator().next(), is("auth0"));
		
		authnFlowMan.updateAuthenticationFlow(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("auth1")));
		authFlows = authnFlowMan.getAuthenticationFlows();
		assertThat(authFlows.size(), is(1));
		assertThat(authFlows.iterator().next().getFirstFactorAuthenticators().iterator().next(), is("auth1"));
	}
	

	@Test
	public void shouldNotRemoveUsedAutheticatorOrFlow() throws Exception
	{
		super.setupMockAuthn();
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 10, RememberMePolicy.disallow , 1, 600);
		realmsMan.addRealm(realm);
		
		Collection<AuthenticatorTypeDescription> authTypes = authnMan
				.getAuthenticatorTypes("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		
		AuthenticatorInstance authInstance1 = authnMan.createAuthenticator(
				"auth0", authType.getId(), "8", "bbb", "credential1");

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
		new EndpointConfiguration(new I18nString("ada"), 
		"ada", new ArrayList<String>(), "",
		realm.getName()));

		authnFlowMan.removeAuthenticationFlow("flow1");
		Collection<AuthenticationFlowDefinition> authFlows = authnFlowMan.getAuthenticationFlows();
		assertThat(authFlows.size(), is(0));

		authnMan.removeAuthenticator(authInstance1.getId());
		Collection<AuthenticatorInstance> auths = authnMan.getAuthenticators(null);
		assertThat(auths.size(), is(0));	
	}
		
	
	@Test
	public void shouldReturnAllCredTypes() throws Exception
	{
		int automaticCredTypes = 3;
		Collection<CredentialType> credTypes = credMan.getCredentialTypes();
		assertEquals(credTypes.toString(), 1+automaticCredTypes, credTypes.size());
		CredentialType credType = getDescObjectByName(credTypes, MockPasswordVerificatorFactory.ID);
		assertEquals(MockPasswordVerificatorFactory.ID, credType.getName());	
	}
	
	private CredentialDefinition addDefaultCredentialDef() throws Exception
	{
		CredentialDefinition credDef = new CredentialDefinition(
				MockPasswordVerificatorFactory.ID, "credential1", 
				new I18nString("cred disp name"),
				new I18nString("cred req desc"));
		credDef.setConfiguration("8");
		credMan.addCredentialDefinition(credDef);
		return credDef;
	}
	
	@Test
	public void credentialCRUDTest() throws Exception
	{
		int automaticCreds = 1;
		//add credential definition
		CredentialDefinition credDef = addDefaultCredentialDef();
		
		//check if is correctly returned
		Collection<CredentialDefinition> credDefs = credMan.getCredentialDefinitions();
		assertEquals(1+automaticCreds, credDefs.size());
		CredentialDefinition credDefRet = getDescObjectByName(credDefs, "credential1");
		assertEquals("credential1", credDefRet.getName());
		assertEquals(new I18nString("cred req desc"), credDefRet.getDescription());
		assertEquals(MockPasswordVerificatorFactory.ID, credDefRet.getTypeId());
		assertEquals("8", credDefRet.getConfiguration());
		
		//update it and check
		credDefRet.setDescription(new I18nString("d2"));
		credDefRet.setConfiguration("9");
		credMan.updateCredentialDefinition(credDefRet, LocalCredentialState.correct);
		credDefs = credMan.getCredentialDefinitions();
		assertEquals(1+automaticCreds, credDefs.size());
		credDefRet = getDescObjectByName(credDefs, "credential1");
		assertEquals("credential1", credDefRet.getName());
		assertEquals("d2", credDefRet.getDescription().getDefaultValue());
		assertEquals(MockPasswordVerificatorFactory.ID, credDefRet.getTypeId());
		assertEquals("9", credDefRet.getConfiguration());
		
		//remove
		credMan.removeCredentialDefinition("credential1");
		credDefs = credMan.getCredentialDefinitions();
		assertEquals(automaticCreds, credDefs.size());

		//add it again
		credMan.addCredentialDefinition(credDef);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldNotRemoveCredentialUsedByAuth() throws Exception
	{
		addDefaultCredentialDef();
		
		Collection<AuthenticatorTypeDescription> authTypes = authnMan
				.getAuthenticatorTypes("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth1",
				authType.getId(), "6", "bbb", "credential1");
		
		credMan.removeCredentialDefinition("credential1");	
	}
	
	@Test
	public void credRequirementCRUDTest() throws Exception
	{
		int automaticCredReqs = 1;
		
		CredentialDefinition credDef = addDefaultCredentialDef();

		CredentialRequirements cr = new CredentialRequirements("crMock", "mock cred req", 
				Collections.singleton(credDef.getName()));
		credReqMan.addCredentialRequirement(cr);
		
		Collection<CredentialRequirements> credReqs = credReqMan.getCredentialRequirements();
		assertEquals(1+automaticCredReqs, credReqs.size());
		CredentialRequirements credReq1 = getDescObjectByName(credReqs, "crMock");
		assertEquals("crMock", credReq1.getName());
		assertEquals("mock cred req", credReq1.getDescription());
		assertEquals(1, credReq1.getRequiredCredentials().size());
		
		credReq1.setDescription("changed");
		credReqMan.updateCredentialRequirement(credReq1);
		credReqs = credReqMan.getCredentialRequirements();
		assertEquals(1+automaticCredReqs, credReqs.size());
		credReq1 = getDescObjectByName(credReqs, "crMock");
		assertEquals("crMock", credReq1.getName());
		assertEquals("changed", credReq1.getDescription());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void shouldNotRemoveCredentialUsedInCredReq() throws Exception
	{	
		CredentialDefinition credDef = addDefaultCredentialDef();
		CredentialRequirements cr = new CredentialRequirements("crMock", "mock cred req", 
				Collections.singleton(credDef.getName()));
		credReqMan.addCredentialRequirement(cr);
		credMan.removeCredentialDefinition("credential1");	
	}
	
	@Test
	public void shouldChangeEnitytCrdentialState() throws Exception
	{
		CredentialDefinition credDef = addDefaultCredentialDef();
		CredentialRequirements cr = new CredentialRequirements("crMock", "mock cred req", 
				Collections.singleton(credDef.getName()));
		credReqMan.addCredentialRequirement(cr);
		
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "CN=test"), 
				"crMock", EntityState.valid, false);
		EntityParam entityP = new EntityParam(id);
		Entity entity = idsMan.getEntity(entityP);
		assertEquals(LocalCredentialState.notSet, entity.getCredentialInfo().
				getCredentialsState().get("credential1").getState());
		
		//set entity credential and check if status notSet was changed to valid
		eCredMan.setEntityCredential(entityP, "credential1", "password");
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().
				get("credential1").getState());

		//update credential requirements and check if the entity has its authN status still fine
		cr.setDescription("changed2");
		credReqMan.updateCredentialRequirement(cr);
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().
				get("credential1").getState());
		
		
		CredentialDefinition credDef2 = new CredentialDefinition(
				MockPasswordVerificatorFactory.ID, "credential2");
		credDef2.setConfiguration("10");
		credMan.addCredentialDefinition(credDef2);
		
		Set<String> set2 = new HashSet<String>();
		Collections.addAll(set2, credDef.getName(), credDef2.getName());
		credReqMan.addCredentialRequirement(new CredentialRequirements("crMock2", "mock cred req2", 
				set2));
		
		eCredMan.setEntityCredentialRequirements(entityP, "crMock2");
		
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().
				get("credential1").getState());
		assertEquals(LocalCredentialState.notSet, entity.getCredentialInfo().getCredentialsState().
				get("credential2").getState());
		eCredMan.setEntityCredential(entityP, "credential2", "password2");
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().
				get("credential1").getState());
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().
				get("credential2").getState());
		
		credReqMan.removeCredentialRequirement("crMock2", "crMock");
		
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().
				get("credential1").getState());
		
	}
	
	@Test(expected = IllegalCredentialException.class)
	public void shouldNotRemoveCredReqWithoutReplacemant() throws Exception
	{
		CredentialDefinition credDef = addDefaultCredentialDef();
		CredentialRequirements cr = new CredentialRequirements("crMock", "mock cred req", 
				Collections.singleton(credDef.getName()));
		credReqMan.addCredentialRequirement(cr);
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "CN=test"), 
				"crMock", EntityState.valid, false);
		EntityParam entityP = new EntityParam(id);
		
		eCredMan.setEntityCredential(entityP, "credential1", "password");
		credReqMan.removeCredentialRequirement(cr.getName(), null);	
	}
	
	@Test
	public void isAdminAllowedToChangeCredential() throws Exception
	{
		setupAdmin();
		setupPasswordAuthn();
		createUsernameUserWithRole(AuthorizationManagerImpl.USER_ROLE);
		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, DEF_USER)); 

		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty").toJson());
	}
	
	@Test
	public void shouldAllowOwnerToChangePassword() throws Exception
	{
		setupPasswordAuthn();
		setupPasswordAndCertAuthn();
		createCertUserNoPassword(AuthorizationManagerImpl.USER_ROLE); //Has no password set, but password is allowed
		setupUserContext("user2", null);
		
		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user2")); 
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty").toJson());
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson());
	}

	@Test
	public void shouldAllowToSetInitialPasswordWithoutThePreviousOne() throws Exception
	{
		setupPasswordAuthn();
		setupPasswordAndCertAuthn();
		createCertUserNoPassword(AuthorizationManagerImpl.USER_ROLE); //Has no password set, but password is allowed
		setupUserContext("user2", null);
		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user2")); 

		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty").toJson());
	}
}
