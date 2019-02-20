/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.stdext.credential.pass.ScryptParams.MIN_WORK_FACTOR;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession.AuthNInfo;
import pl.edu.icm.unity.engine.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.authz.InternalAuthorizationManagerImpl;
import pl.edu.icm.unity.engine.mock.MockPasswordVerificatorFactory;
import pl.edu.icm.unity.exceptions.CredentialRecentlyUsedException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredential;
import pl.edu.icm.unity.stdext.credential.pass.PasswordToken;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.stdext.credential.pass.ScryptParams;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.store.api.generic.AuthenticatorConfigurationDB;
import pl.edu.icm.unity.store.api.tx.TransactionalRunner;
import pl.edu.icm.unity.store.types.AuthenticatorConfiguration;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;
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

//TODO bit messy: cred req and cred man, tests needs refactoring
public class CredentialManagementTest extends DBIntegrationTestBase
{
	@Autowired
	private AuthenticatorManagement authnMan;
	
	@Autowired
	private AuthenticatorConfigurationDB authenticatorDB;
	
	@Autowired
	private AuthenticatorsRegistry authenticatorsReg;
	
	@Autowired
	private TransactionalRunner tx;
		
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
		
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg.getAuthenticatorTypesByBinding("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth1",
				authType.getVerificationMethod(), "bbb", "credential1");
		
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
		createUsernameUserWithRole(InternalAuthorizationManagerImpl.USER_ROLE);
		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, DEF_USER)); 

		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty").toJson());
	}
	
	@Test
	public void shouldAllowOwnerToChangePassword() throws Exception
	{
		setupPasswordAuthn();
		setupPasswordAndCertAuthn();
		createCertUserNoPassword(InternalAuthorizationManagerImpl.USER_ROLE); //Has no password set, but password is allowed
		AuthenticatorInstance authenticator = getAuthenticator("authn", "credential1"); 
		AuthenticationFlow flow = new AuthenticationFlow("flow", Policy.NEVER, Sets.newHashSet(authenticator), 
				Collections.emptyList(), 1);
		setupUserContext(sessionMan, identityResolver, "user2", null, Lists.newArrayList(flow));
		
		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user2")); 
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty").toJson());
		
		InvocationContext.getCurrent().getLoginSession().setAdditionalAuthn(new AuthNInfo("authn", new Date()));
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson());
	}

	private AuthenticatorInstance getAuthenticator(String authenticator, String credential)
	{
		AuthenticatorInstance auth1 = mock(AuthenticatorInstance.class);
		AuthenticatorInstanceMetadata instance1 = mock(AuthenticatorInstanceMetadata.class);
		when(instance1.getLocalCredentialName()).thenReturn(credential);
		when(instance1.getId()).thenReturn(authenticator);
		when(auth1.getMetadata()).thenReturn(instance1);
		CredentialRetrieval retrieval = mock(CredentialRetrieval.class);
		when(auth1.getRetrieval()).thenReturn(retrieval);
		return auth1;
	}
	
	@Test
	public void shouldAllowToSetInitialPasswordWithoutThePreviousOne() throws Exception
	{
		setupPasswordAuthn();
		setupPasswordAndCertAuthn();
		createCertUserNoPassword(InternalAuthorizationManagerImpl.USER_ROLE); //Has no password set, but password is allowed
		setupUserContext("user2", null);
		EntityParam user = new EntityParam(new IdentityTaV(UsernameIdentity.ID, "user2")); 

		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty").toJson());
	}
	

	@Test
	public void shouldFailToSetCurrentPasswordFromHistory() throws Exception
	{
		PasswordCredential passConfig = new PasswordCredential();
		passConfig.setHistorySize(1);
		passConfig.setScryptParams(new ScryptParams(MIN_WORK_FACTOR));
		passConfig.setMinScore(1);
		createPassCredentialAndCR("credential1", passConfig);
		
		EntityParam user = new EntityParam(idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user"), 
				CRED_REQ_PASS, EntityState.valid, false));
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty1").toJson());
		
		Throwable error = catchThrowable(() -> eCredMan.setEntityCredential(
				user, "credential1", new PasswordToken("qw!Erty1").toJson()));
		
		assertThat(error).isInstanceOf(CredentialRecentlyUsedException.class);
	}

	@Test
	public void shouldFailToSetOldPasswordFromHistory() throws Exception
	{
		PasswordCredential passConfig = new PasswordCredential();
		passConfig.setHistorySize(2);
		passConfig.setScryptParams(new ScryptParams(MIN_WORK_FACTOR));
		passConfig.setMinScore(1);
		createPassCredentialAndCR("credential1", passConfig);
		
		EntityParam user = new EntityParam(idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user"), 
				CRED_REQ_PASS, EntityState.valid, false));
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty1").toJson());
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson());
		
		Throwable error = catchThrowable(() -> eCredMan.setEntityCredential(
				user, "credential1", new PasswordToken("qw!Erty1").toJson()));
		
		assertThat(error).isInstanceOf(CredentialRecentlyUsedException.class);
	}
	
	@Test
	public void shouldSetOldPasswordPushedOutFromHistory() throws Exception
	{
		PasswordCredential passConfig = new PasswordCredential();
		passConfig.setHistorySize(1);
		passConfig.setScryptParams(new ScryptParams(MIN_WORK_FACTOR));
		passConfig.setMinScore(1);
		createPassCredentialAndCR("credential1", passConfig);
		
		EntityParam user = new EntityParam(idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user"), 
				CRED_REQ_PASS, EntityState.valid, false));
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty1").toJson());
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson());
		
		Throwable error = catchThrowable(() -> eCredMan.setEntityCredential(
				user, "credential1", new PasswordToken("qw!Erty1").toJson()));
		
		assertThat(error).isNull();
	}
	
	@Test
	public void shouldClearAllHistoricalPasswordsAfterStorageSchemeReconfiguration() throws Exception
	{
		PasswordCredential passConfig = new PasswordCredential();
		passConfig.setHistorySize(2);
		passConfig.setScryptParams(new ScryptParams(MIN_WORK_FACTOR));
		passConfig.setMinScore(1);
		createPassCredentialAndCR("credential1", passConfig);
		
		EntityParam user = new EntityParam(idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user"), 
				CRED_REQ_PASS, EntityState.valid, false));
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty1").toJson());
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson());
		
		passConfig.setScryptParams(new ScryptParams(MIN_WORK_FACTOR+1));
		updatePassCredential("credential1", passConfig);
		
		Throwable error = catchThrowable(() -> eCredMan.setEntityCredential(
				user, "credential1", new PasswordToken("qw!Erty1").toJson()));
		assertThat(error).isNull();
	}

	@Test
	public void shouldNotTrimHistoricalPasswordsAfterNotAffectingReconfiguration() throws Exception
	{
		PasswordCredential passConfig = new PasswordCredential();
		passConfig.setHistorySize(2);
		passConfig.setScryptParams(new ScryptParams(MIN_WORK_FACTOR));
		passConfig.setMinScore(1);
		createPassCredentialAndCR("credential1", passConfig);
		
		EntityParam user = new EntityParam(idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user"), 
				CRED_REQ_PASS, EntityState.valid, false));
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty1").toJson());
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson());
		
		passConfig.setMinScore(2);
		updatePassCredential("credential1", passConfig);
		
		Throwable error = catchThrowable(() -> eCredMan.setEntityCredential(
				user, "credential1", new PasswordToken("qw!Erty1").toJson()));
		assertThat(error).isInstanceOf(CredentialRecentlyUsedException.class);
	}

	@Test
	public void shouldTrimHistoricalPasswordsAfterHistoryDecrease() throws Exception
	{
		PasswordCredential passConfig = new PasswordCredential();
		passConfig.setHistorySize(3);
		passConfig.setScryptParams(new ScryptParams(MIN_WORK_FACTOR));
		passConfig.setMinScore(1);
		createPassCredentialAndCR("credential1", passConfig);
		
		EntityParam user = new EntityParam(idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user"), 
				CRED_REQ_PASS, EntityState.valid, false));
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty1").toJson());
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson());
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty3").toJson());
		
		passConfig.setHistorySize(2);
		updatePassCredential("credential1", passConfig);
		
		Throwable error = catchThrowable(() -> eCredMan.setEntityCredential(
				user, "credential1", new PasswordToken("qw!Erty1").toJson()));
		assertThat(error).isNull();
	}

	//bit tricky test: set limit to have 3 in history and fill it. Then reduce it to 2 and go back to 3.
	//we expect that the reduction to 2 should trim history by one.
	@Test
	public void shouldNotStoreHistoryBeyondChangedLimit() throws Exception
	{
		PasswordCredential passConfig = new PasswordCredential();
		passConfig.setHistorySize(3);
		passConfig.setScryptParams(new ScryptParams(MIN_WORK_FACTOR));
		passConfig.setMinScore(1);

		createPassCredentialAndCR("credential1", passConfig);
		
		EntityParam user = new EntityParam(idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user"), 
				CRED_REQ_PASS, EntityState.valid, false));
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty1").toJson());
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty2").toJson());
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty3").toJson());
		
		passConfig.setHistorySize(2);
		updatePassCredential("credential1", passConfig);
		
		passConfig.setHistorySize(3);
		updatePassCredential("credential1", passConfig);
		
		Throwable error = catchThrowable(() -> eCredMan.setEntityCredential(
				user, "credential1", new PasswordToken("qw!Erty1").toJson()));
		assertThat(error).isNull();
		Throwable error2 = catchThrowable(() -> eCredMan.setEntityCredential(
				user, "credential1", new PasswordToken("qw!Erty2").toJson()));
		assertThat(error2).isInstanceOf(CredentialRecentlyUsedException.class);
	}

	@Test
	public void shouldNeverTrimCurrentPassword() throws Exception
	{
		PasswordCredential passConfig = new PasswordCredential();
		passConfig.setHistorySize(1);
		passConfig.setScryptParams(new ScryptParams(MIN_WORK_FACTOR));
		passConfig.setMinScore(1);
		createPassCredentialAndCR("credential1", passConfig);
		
		EntityParam user = new EntityParam(idsMan.addEntity(new IdentityParam(UsernameIdentity.ID, "user"), 
				CRED_REQ_PASS, EntityState.valid, false));
		
		eCredMan.setEntityCredential(user, "credential1", new PasswordToken("qw!Erty1").toJson());
		
		passConfig.setHistorySize(0);
		updatePassCredential("credential1", passConfig);
		passConfig.setHistorySize(1);
		updatePassCredential("credential1", passConfig);
		
		Throwable error = catchThrowable(() -> eCredMan.setEntityCredential(
				user, "credential1", new PasswordToken("qw!Erty1").toJson()));
		assertThat(error).isInstanceOf(CredentialRecentlyUsedException.class);
	}

	@Test
	public void shouldRefreshAuthenticatorAfterItsCredentialChange() throws Exception
	{
		addDefaultCredentialDef();
		
		Collection<AuthenticatorTypeDescription> authTypes = authenticatorsReg.getAuthenticatorTypesByBinding("web");
		AuthenticatorTypeDescription authType = authTypes.iterator().next();
		authnMan.createAuthenticator("auth1", authType.getVerificationMethod(), "bbb", "credential1");
		AuthenticatorConfiguration authenticatorInitial = tx.runInTransactionRet(
				() -> authenticatorDB.get("auth1"));
		
		CredentialDefinition credDef = new CredentialDefinition(
				MockPasswordVerificatorFactory.ID, "credential1", 
				new I18nString("cred disp name"),
				new I18nString("cred req desc"));
		credDef.setConfiguration("888");
		credMan.updateCredentialDefinition(credDef, LocalCredentialState.outdated);

		AuthenticatorConfiguration authenticatorUpdated = tx.runInTransactionRet(
				() -> authenticatorDB.get("auth1"));
		assertThat(authenticatorUpdated.getRevision(), is(authenticatorInitial.getRevision()+1));
	}

	
	private void createPassCredentialAndCR(String credential, PasswordCredential passConfig) throws Exception
	{
		CredentialDefinition credDef = new CredentialDefinition(PasswordVerificator.NAME, credential);
		credDef.setConfiguration(JsonUtil.serialize(passConfig.getSerializedConfiguration()));
		credMan.addCredentialDefinition(credDef);
		
		CredentialRequirements cr = new CredentialRequirements(CRED_REQ_PASS, "", 
				Collections.singleton(credDef.getName()));
		credReqMan.addCredentialRequirement(cr);
	}
	
	private void updatePassCredential(String credential, PasswordCredential passConfig) throws Exception
	{
		CredentialDefinition credDef = new CredentialDefinition(PasswordVerificator.NAME, credential);
		credDef.setConfiguration(JsonUtil.serialize(passConfig.getSerializedConfiguration()));
		credMan.updateCredentialDefinition(credDef, LocalCredentialState.outdated);
	}
}







