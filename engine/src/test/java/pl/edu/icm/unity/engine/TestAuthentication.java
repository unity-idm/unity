/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.IllegalPreviousCredentialException;
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
				10, 10, -1, 600);
		realmsMan.addRealm(realm);
		
		Identity id = idsMan.addEntity(new IdentityParam(X500Identity.ID, "CN=foo"), 
				"crMock", EntityState.valid, false);

		//create authenticator and an endpoint with it
		Collection<AuthenticatorTypeDescription> authTypes = authnMan.getAuthenticatorTypes("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth1", authType.getId(), "6", "bbb", "credential1");
		
		authnFlowMan.addAuthenticationFlowDefinition(new AuthenticationFlowDefinition(
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
	
	@Test
	public void testAuthnManagement() throws Exception
	{
		//create credential definition
		super.setupMockAuthn();
		AuthenticationRealm realm = new AuthenticationRealm("testr", "", 
				10, 10, -1, 600);
		realmsMan.addRealm(realm);

		//get authn types
		Collection<AuthenticatorTypeDescription> authTypes = authnMan.getAuthenticatorTypes("web");
		assertEquals(1, authTypes.size());
		authTypes = authnMan.getAuthenticatorTypes(null);
		assertEquals(1, authTypes.size());
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		assertEquals(true, authType.isLocal());
		assertEquals("mockretrieval", authType.getRetrievalMethod());
		assertEquals(MockPasswordVerificatorFactory.ID, authType.getVerificationMethod());
		assertEquals("web", authType.getSupportedBinding());
		
		//create authenticator
		AuthenticatorInstance authInstance = authnMan.createAuthenticator(
				"auth1", authType.getId(), "8", "bbb", "credential1");

		//get authenticators
		Collection<AuthenticatorInstance> auths = authnMan.getAuthenticators("web");
		assertEquals(1, auths.size());
		AuthenticatorInstance authInstanceR = auths.iterator().next();
		assertEquals("auth1", authInstanceR.getId());
		assertEquals("bbb", authInstanceR.getRetrievalConfiguration());
		assertNull(authInstanceR.getVerificatorConfiguration());
		
		//update authenticator
		authnMan.updateAuthenticator("auth1", "9", "b", "credential1");

		auths = authnMan.getAuthenticators("web");
		assertEquals(1, auths.size());
		authInstanceR = auths.iterator().next();
		assertEquals("auth1", authInstanceR.getId());
		assertEquals("b", authInstanceR.getRetrievalConfiguration());
		assertNull(authInstanceR.getVerificatorConfiguration());
		
		//create authentication flow
		authnFlowMan.addAuthenticationFlowDefinition(new AuthenticationFlowDefinition(
				"flow1", Policy.NEVER, Sets.newHashSet("auth1")));
		Collection<AuthenticationFlowDefinition> authFlows = authnFlowMan.getAuthenticationFlows();
		assertEquals(1, authFlows.size());
		
		//create endpoint
		List<EndpointTypeDescription> endpointTypes = endpointMan.getEndpointTypes();
		assertEquals(1, endpointTypes.size());
		EndpointTypeDescription type = endpointTypes.get(0);
		
		EndpointConfiguration cfg = new EndpointConfiguration(new I18nString("endpoint1"),
				"desc", new ArrayList<String>(), "", realm.getName());
		endpointMan.deploy(type.getName(), "endpoint1", "/foo", cfg);
		List<ResolvedEndpoint> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		//and assign the authentication flow to it
		endpointMan.updateEndpoint(endpoints.get(0).getEndpoint().getName(), 
				new EndpointConfiguration(new I18nString("ada"), 
				"ada", Collections.singletonList("flow1"), "", realm.getName()));

		//check if is returned
		List<String> authSets = endpointMan.getEndpoints().get(0).
				getEndpoint().getConfiguration().getAuthenticationOptions();
		assertEquals(1, authSets.size());
		
		//remove a used authenticator
		try
		{
			authnMan.removeAuthenticator(authInstance.getId());
			fail("Was able to remove a used authenticator");
		} catch (IllegalArgumentException e) {}
		
		//remove it from endpoint
		endpointMan.updateEndpoint(endpoints.get(0).getEndpoint().getName(), 
				new EndpointConfiguration(new I18nString("ada"), 
				"ada", new ArrayList<String>(), "",
				realm.getName()));
		
		
		authnFlowMan.removeAuthenticationFlowDefinition("flow1");
		authFlows= authnFlowMan.getAuthenticationFlows();
		assertEquals(0, authFlows.size());		
		
		//remove again
		authnMan.removeAuthenticator(authInstance.getId());
		auths = authnMan.getAuthenticators(null);
		assertEquals(0, auths.size());		
		
		
		
	}
	
	@Test
	public void testCredentialsManagement() throws Exception
	{
		int automaticCredTypes = 3;
		int automaticCreds = 1;
		int automaticCredReqs = 1;
		//check if credential types are returned
		Collection<CredentialType> credTypes = credMan.getCredentialTypes();
		assertEquals(credTypes.toString(), 1+automaticCredTypes, credTypes.size());
		CredentialType credType = getDescObjectByName(credTypes, MockPasswordVerificatorFactory.ID);
		assertEquals(MockPasswordVerificatorFactory.ID, credType.getName());
		
		//add credential definition
		CredentialDefinition credDef = new CredentialDefinition(
				MockPasswordVerificatorFactory.ID, "credential1", 
				new I18nString("cred disp name"),
				new I18nString("cred req desc"));
		credDef.setConfiguration("8");
		credMan.addCredentialDefinition(credDef);
		
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

		//add authenticator with it and try to remove
		Collection<AuthenticatorTypeDescription> authTypes = authnMan.getAuthenticatorTypes("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		AuthenticatorInstance authInstance = authnMan.createAuthenticator(
				"auth1", authType.getId(), "6", "bbb", "credential1");
		try
		{
			credMan.removeCredentialDefinition("credential1");
			fail("Managed to remove credential used by authenticator");
		} catch (IllegalArgumentException e) {}
		authnMan.removeAuthenticator(authInstance.getId());
		
		
		//add credential requirements
		CredentialRequirements cr = new CredentialRequirements("crMock", "mock cred req", 
				Collections.singleton(credDef.getName()));
		credReqMan.addCredentialRequirement(cr);
		
		//check if are correctly returned
		Collection<CredentialRequirements> credReqs = credReqMan.getCredentialRequirements();
		assertEquals(1+automaticCredReqs, credReqs.size());
		CredentialRequirements credReq1 = getDescObjectByName(credReqs, "crMock");
		assertEquals("crMock", credReq1.getName());
		assertEquals("mock cred req", credReq1.getDescription());
		assertEquals(1, credReq1.getRequiredCredentials().size());
		
		//update credential requirements and check
		credReq1.setDescription("changed");
		credReqMan.updateCredentialRequirement(credReq1);
		credReqs = credReqMan.getCredentialRequirements();
		assertEquals(1+automaticCredReqs, credReqs.size());
		credReq1 = getDescObjectByName(credReqs, "crMock");
		assertEquals("crMock", credReq1.getName());
		assertEquals("changed", credReq1.getDescription());
		
		//try to remove credential - now with cred req
		try
		{
			credMan.removeCredentialDefinition("credential1");
			fail("Managed to remove credential used by cred req");
		} catch (IllegalArgumentException e) {}
		
		//add identity with cred requirements with notSet state
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
		credReq1.setDescription("changed2");
		credReqMan.updateCredentialRequirement(credReq1);
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().
				get("credential1").getState());

		//update credential definition now with identity using it via credential requirements
		credDefRet.setDescription(new I18nString("d3"));
		credDefRet.setConfiguration("119");
		credMan.updateCredentialDefinition(credDefRet, LocalCredentialState.correct);
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().
				get("credential1").getState());
		
		try
		{
			credReqMan.removeCredentialRequirement(credReq1.getName(), null);
			fail("Managed to remove used requirements without replacement");
		} catch (IllegalCredentialException e) {}

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
		credReqs = credReqMan.getCredentialRequirements();
		assertEquals(1+automaticCredReqs, credReqs.size());
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().
				get("credential1").getState());
	}
	
	@Test
	public void isAdminAllowedToChangeCredential() throws Exception
	{
		setupAdmin();
		setupPasswordAuthn();
		createUsernameUserWithRole(AuthorizationManagerImpl.USER_ROLE);
		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, DEF_USER)); 
		assertFalse(eCredMan.isCurrentCredentialRequiredForChange(user, "credential1"));
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty").toJson());
	}
	
	@Test
	public void isUserRequiredToProvideCurrentCredentialUponChange() throws Exception
	{
		setupPasswordAuthn();
		setupPasswordAndCertAuthn();
		createCertUserNoPassword(AuthorizationManagerImpl.USER_ROLE); //Has no password set, but password is allowed
		setupUserContext("user2", null);
		
		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user2")); 
		assertFalse(eCredMan.isCurrentCredentialRequiredForChange(user, "credential1"));
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty").toJson());
		try
		{
			eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson());
			fail("Managed to change the password without giving the old one");
		} catch (IllegalPreviousCredentialException e)
		{
			//OK - expected
		}
		try
		{
			eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson(),
					new PasswordToken("INVALID").toJson());
			fail("Managed to change the password with invalid old one");
		} catch (IllegalPreviousCredentialException e)
		{
			//OK - expected
		}
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson(),
				new PasswordToken("qw!Erty").toJson());
	}
}
