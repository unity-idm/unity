/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import static org.junit.Assert.*;

import pl.edu.icm.unity.engine.mock.MockEndpoint;
import pl.edu.icm.unity.engine.mock.MockEndpointFactory;
import pl.edu.icm.unity.engine.mock.MockPasswordHandlerFactory;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.authn.AuthenticatorTypeDescription;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.CredentialRequirements;
import pl.edu.icm.unity.types.authn.CredentialType;
import pl.edu.icm.unity.types.authn.LocalAuthenticationState;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

public class TestAuthentication extends DBIntegrationTestBase
{
	@Test
	public void testAuthentication() throws Exception
	{
		//create credential requirement and an identity with it 
		super.setupAuthn();
		
		Identity id = idsMan.addIdentity(new IdentityParam(X500Identity.ID, "CN=foo", true, false), 
				"crMock", LocalAuthenticationState.outdated);

		//create authenticator and an endpoint with it
		Collection<AuthenticatorTypeDescription> authTypes = authnMan.getAuthenticatorTypes("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth1", authType.getId(), "aaa", "bbb", "credential1");
		
		AuthenticatorSet authSet = new AuthenticatorSet(Collections.singleton("auth1"));
		endpointMan.deploy(MockEndpointFactory.NAME, "endpoint1", "/foo", "desc", 
				Collections.singletonList(authSet), "");

		//set wrong password 
		EntityParam entityP = new EntityParam(id);
		idsMan.setEntityCredential(entityP, "credential1", "password");
		
		MockEndpoint endpoint = (MockEndpoint) httpServer.getDeployedEndpoints().iterator().next();
		try
		{
			endpoint.authenticate();
			fail("Authn with wrong cred succeeded");
		} catch (IllegalCredentialException e) {}
		
		//set correct password 
		idsMan.setEntityCredential(entityP, "credential1", "bar");
		long entityId = endpoint.authenticate();
		Entity entity = idsMan.getEntity(entityP);
		assertEquals(entityId+"", entity.getId());
	}
	
	@Test
	public void testAuthnManagement() throws Exception
	{
		//create credential definition
		super.setupAuthn();

		//get authn types
		Collection<AuthenticatorTypeDescription> authTypes = authnMan.getAuthenticatorTypes("web");
		assertEquals(1, authTypes.size());
		authTypes = authnMan.getAuthenticatorTypes(null);
		assertEquals(1, authTypes.size());
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		assertEquals(true, authType.isLocal());
		assertEquals("mockretrieval", authType.getRetrievalMethod());
		assertEquals("mockpasswd", authType.getVerificationMethod());
		assertEquals("web", authType.getSupportedBinding());
		
		//create authenticator
		AuthenticatorInstance authInstance = authnMan.createAuthenticator(
				"auth1", authType.getId(), "aaa", "bbb", "credential1");

		//get authenticators
		Collection<AuthenticatorInstance> auths = authnMan.getAuthenticators("web");
		assertEquals(1, auths.size());
		AuthenticatorInstance authInstanceR = auths.iterator().next();
		assertEquals("auth1", authInstanceR.getId());
		assertEquals("bbb", authInstanceR.getRetrievalJsonConfiguration());
		assertEquals("aaa", authInstanceR.getVerificatorJsonConfiguration());
		
		//update authenticator
		authnMan.updateAuthenticator("auth1", "a", "b");

		auths = authnMan.getAuthenticators("web");
		assertEquals(1, auths.size());
		authInstanceR = auths.iterator().next();
		assertEquals("auth1", authInstanceR.getId());
		assertEquals("b", authInstanceR.getRetrievalJsonConfiguration());
		assertEquals("a", authInstanceR.getVerificatorJsonConfiguration());
		
		//create endpoint
		List<EndpointTypeDescription> endpointTypes = endpointMan.getEndpointTypes();
		assertEquals(1, endpointTypes.size());
		EndpointTypeDescription type = endpointTypes.get(0);
		
		endpointMan.deploy(type.getName(), "endpoint1", "/foo", "desc", new ArrayList<AuthenticatorSet>(), "");
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		//and assign the authenticator to it
		AuthenticatorSet authSet = new AuthenticatorSet(Collections.singleton("auth1"));
		endpointMan.updateEndpoint(endpoints.get(0).getId(), "ada", Collections.singletonList(authSet), "");

		//check if is returned
		List<AuthenticatorSet> authSets = endpointMan.getEndpoints().get(0).getAuthenticatorSets();
		assertEquals(1, authSets.size());
		assertEquals(1, authSets.get(0).getAuthenticators().size());
		
		//remove a used authenticator
		try
		{
			authnMan.removeAuthenticator(authInstance.getId());
			fail("Was able to remove a used authenticator");
		} catch (IllegalArgumentException e) {}
		
		//remove it from endpoint
		endpointMan.updateEndpoint(endpoints.get(0).getId(), "ada", new ArrayList<AuthenticatorSet>(), "");
		
		//remove again
		authnMan.removeAuthenticator(authInstance.getId());
		auths = authnMan.getAuthenticators(null);
		assertEquals(0, auths.size());		
	}
	
	@Test
	public void testCredentialsManagement() throws Exception
	{
		//check if credential types are returned
		Collection<CredentialType> credTypes = authnMan.getCredentialTypes();
		assertEquals(1, credTypes.size());
		CredentialType credType = credTypes.iterator().next();
		assertEquals(MockPasswordHandlerFactory.ID, credType.getName());
		
		//add credential definition
		CredentialDefinition credDef = new CredentialDefinition(
				MockPasswordHandlerFactory.ID, "credential1", "cred req desc");
		credDef.setJsonConfiguration("8");
		authnMan.addCredentialDefinition(credDef);
		
		//check if is correctly returned
		Collection<CredentialDefinition> credDefs = authnMan.getCredentialDefinitions();
		assertEquals(1, credDefs.size());
		CredentialDefinition credDefRet = credDefs.iterator().next();
		assertEquals("credential1", credDefRet.getName());
		assertEquals("cred req desc", credDefRet.getDescription());
		assertEquals(MockPasswordHandlerFactory.ID, credDefRet.getTypeId());
		assertEquals("8", credDefRet.getJsonConfiguration());
		
		//update it and check
		credDefRet.setDescription("d2");
		credDefRet.setJsonConfiguration("9");
		authnMan.updateCredentialDefinition(credDefRet, LocalAuthenticationState.valid);
		credDefs = authnMan.getCredentialDefinitions();
		assertEquals(1, credDefs.size());
		credDefRet = credDefs.iterator().next();
		assertEquals("credential1", credDefRet.getName());
		assertEquals("d2", credDefRet.getDescription());
		assertEquals(MockPasswordHandlerFactory.ID, credDefRet.getTypeId());
		assertEquals("9", credDefRet.getJsonConfiguration());
		
		//remove
		authnMan.removeCredentialDefinition("credential1");
		credDefs = authnMan.getCredentialDefinitions();
		assertEquals(0, credDefs.size());

		//add it again
		authnMan.addCredentialDefinition(credDef);

		//add authenticator with it and try to remove
		Collection<AuthenticatorTypeDescription> authTypes = authnMan.getAuthenticatorTypes("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		AuthenticatorInstance authInstance = authnMan.createAuthenticator(
				"auth1", authType.getId(), "aaa", "bbb", "credential1");
		try
		{
			authnMan.removeCredentialDefinition("credential1");
			fail("Managed to remove credential used by authenticator");
		} catch (IllegalCredentialException e) {}
		authnMan.removeAuthenticator(authInstance.getId());
		
		
		//add credential requirements
		CredentialRequirements cr = new CredentialRequirements("crMock", "mock cred req", 
				Collections.singleton(credDef.getName()));
		authnMan.addCredentialRequirement(cr);
		
		//check if are correctly returned
		Collection<CredentialRequirements> credReqs = authnMan.getCredentialRequirements();
		assertEquals(1, credReqs.size());
		CredentialRequirements credReq1 = credReqs.iterator().next();
		assertEquals("crMock", credReq1.getName());
		assertEquals("mock cred req", credReq1.getDescription());
		assertEquals(1, credReq1.getRequiredCredentials().size());
		
		//update credential requirements and check
		credReq1.setDescription("changed");
		authnMan.updateCredentialRequirement(credReq1, LocalAuthenticationState.valid);
		credReqs = authnMan.getCredentialRequirements();
		assertEquals(1, credReqs.size());
		credReq1 = credReqs.iterator().next();
		assertEquals("crMock", credReq1.getName());
		assertEquals("changed", credReq1.getDescription());
		
		//try to remove credential - now with cred req
		try
		{
			authnMan.removeCredentialDefinition("credential1");
			fail("Managed to remove credential used by cred req");
		} catch (IllegalCredentialException e) {}
		
		//add identity with cred requirements with outdated state
		Identity id = idsMan.addIdentity(new IdentityParam(X500Identity.ID, "CN=test", true, false), 
				"crMock", LocalAuthenticationState.outdated);
		EntityParam entityP = new EntityParam(id);
		Entity entity = idsMan.getEntity(entityP);
		assertEquals(LocalAuthenticationState.outdated, entity.getCredentialInfo().getAuthenticationState());
		
		//set entity credential and check if status outdated was changed to valid
		idsMan.setEntityCredential(entityP, "credential1", "password");
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalAuthenticationState.valid, entity.getCredentialInfo().getAuthenticationState());

		//update credential requirements and check if the entity has its authN status changed
		credReq1.setDescription("changed2");
		authnMan.updateCredentialRequirement(credReq1, LocalAuthenticationState.disabled);
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalAuthenticationState.disabled, entity.getCredentialInfo().getAuthenticationState());

		//update credential now with identity using it via credential requirements
		credDefRet.setDescription("d3");
		credDefRet.setJsonConfiguration("119");
		authnMan.updateCredentialDefinition(credDefRet, LocalAuthenticationState.valid);
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalAuthenticationState.valid, entity.getCredentialInfo().getAuthenticationState());
		
		try
		{
			authnMan.removeCredentialRequirement(credReq1.getName(), null, null);
			fail("Managed to remove used requirements without replacement");
		} catch (IllegalCredentialException e) {}

		CredentialDefinition credDef2 = new CredentialDefinition(
				MockPasswordHandlerFactory.ID, "credential2", "cred2 desc");
		credDef2.setJsonConfiguration("10");
		authnMan.addCredentialDefinition(credDef2);
		
		Set<String> set2 = new HashSet<String>();
		Collections.addAll(set2, credDef.getName(), credDef2.getName());
		authnMan.addCredentialRequirement(new CredentialRequirements("crMock2", "mock cred req2", 
				set2));
		
		try
		{
			idsMan.setEntityCredentialRequirements(entityP, "crMock2", LocalAuthenticationState.valid);
			fail("Managed to set valid state for invalid cred req");
		} catch (IllegalCredentialException e) {}
		idsMan.setEntityCredentialRequirements(entityP, "crMock2", LocalAuthenticationState.disabled);
		
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalAuthenticationState.disabled, entity.getCredentialInfo().getAuthenticationState());
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().get("credential1"));
		assertEquals(LocalCredentialState.notSet, entity.getCredentialInfo().getCredentialsState().get("credential2"));
		idsMan.setEntityCredential(entityP, "credential2", "password2");
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalAuthenticationState.disabled, entity.getCredentialInfo().getAuthenticationState());
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().get("credential1"));
		assertEquals(LocalCredentialState.correct, entity.getCredentialInfo().getCredentialsState().get("credential2"));
		
		authnMan.removeCredentialRequirement("crMock2", "crMock", LocalAuthenticationState.disabled);
		credReqs = authnMan.getCredentialRequirements();
		assertEquals(1, credReqs.size());
		entity = idsMan.getEntity(entityP);
		assertEquals(LocalAuthenticationState.disabled, entity.getCredentialInfo().getAuthenticationState());

	}
}
