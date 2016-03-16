/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.builders.RegistrationRequestBuilder;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfileBuilder;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.translation.TranslationProfile;

import com.google.common.collect.Lists;

public class TestRegistrations extends DBIntegrationTestBase
{
	@Autowired
	private InitializerCommon commonInitializer;
	
	@Autowired
	private RegistrationActionsRegistry registry;

	@Test 
	public void addedFormIsReturned() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		List<RegistrationForm> forms = registrationsMan.getForms();
		assertEquals(1, forms.size());
		assertEquals(form, forms.get(0));
	}
	
	@Test 
	public void removedFormIsNotReturned() throws Exception
	{
		initAndCreateForm(false, null);
		
		registrationsMan.removeForm("f1", false);
		
		assertEquals(0, registrationsMan.getForms().size());
	}

	@Test 
	public void missingFormCantBeRemoved() throws Exception
	{
		try
		{
			registrationsMan.removeForm("mising", true);
			fail("Removed non existing form");
		} catch (WrongArgumentException e) {/*ok*/}
	}
	
	@Test 
	public void formWithDuplicateNameCantBeAdded() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		try
		{
			registrationsMan.addForm(form);
			fail("Added the same form twice");
		} catch (WrongArgumentException e) {/*ok*/}
	}
	
	@Test 
	public void formWithMissingAttributeCantBeAdded() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		RegistrationFormBuilder testFormBuilder = getFormBuilder(false, null, true);

		AttributeRegistrationParam attrReg = form.getAttributeParams().get(0);
		attrReg.setAttributeType("missing");
		testFormBuilder.withAttributeParams(Collections.singletonList(attrReg));
		
		checkUpdateOrAdd(testFormBuilder.build(), "attr(2)", WrongArgumentException.class);
	}
	
	@Test 
	public void formWithMissingCredentialCantBeAdded() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		RegistrationFormBuilder testFormBuilder = getFormBuilder(false, null, true);

		CredentialRegistrationParam credParam = form.getCredentialParams().get(0);
		credParam.setCredentialName("missing");
		testFormBuilder.withCredentialParams(Collections.singletonList(credParam));
		
		checkUpdateOrAdd(testFormBuilder.build(), "cred", WrongArgumentException.class);
	}

	
	@Test 
	public void formWithMissingCredentialReqCantBeAdded() throws Exception
	{
		initAndCreateForm(false, null);
		RegistrationFormBuilder testFormBuilder = getFormBuilder(false, null, true);
		testFormBuilder.withDefaultCredentialRequirement("missing");
		checkUpdateOrAdd(testFormBuilder.build(), "cred req", WrongArgumentException.class);
	}
	
	@Test 
	public void formWithMissingGroupCantBeAdded() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		RegistrationFormBuilder testFormBuilder = getFormBuilder(false, null, true);

		GroupRegistrationParam groupParam = form.getGroupParams().get(0);
		groupParam.setGroupPath("/missing");
		testFormBuilder.withGroupParams(Collections.singletonList(groupParam));
		checkUpdateOrAdd(testFormBuilder.build(), "group", IllegalGroupValueException.class);
	}

	@Test 
	public void formWithMissingIdentityCantBeAdded() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		RegistrationFormBuilder testFormBuilder = getFormBuilder(false, null, true);
		IdentityRegistrationParam idParam = form.getIdentityParams().get(0);
		idParam.setIdentityType("missing");
		testFormBuilder.withIdentityParams(Collections.singletonList(idParam));
		checkUpdateOrAdd(testFormBuilder.build(), "id", IllegalTypeException.class);
	}

	@Test
	public void formWithRequestCantBeUpdated() throws Exception
	{
		initAndCreateForm(false, null);
		
		registrationsMan.submitRegistrationRequest(getRequest(), 
				new RegistrationContext(false, false, TriggeringMode.manualAtLogin));
		
		try
		{
			registrationsMan.updateForm(getFormBuilder(false, null, true).build(), false);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
	}

	@Test
	public void formWithRequestCanBeForcedToBeUpdated() throws Exception
	{
		initAndCreateForm(false, null);
		
		registrationsMan.submitRegistrationRequest(getRequest(), 
				new RegistrationContext(false, false, TriggeringMode.manualAtLogin));
		
		registrationsMan.updateForm(getFormBuilder(false, null, true).build(), true);
	}

	@Test
	public void formWithRequestCantBeRemoved() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		
		registrationsMan.submitRegistrationRequest(getRequest(), 
				new RegistrationContext(false, false, TriggeringMode.manualAtLogin));
		
		try
		{
			registrationsMan.removeForm(form.getName(), false);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		assertEquals(1, registrationsMan.getRegistrationRequests().size());
	}

	@Test
	public void formWithRequestCanBeForcedToBeRemoved() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		
		registrationsMan.submitRegistrationRequest(getRequest(), 
				new RegistrationContext(false, false, TriggeringMode.manualAtLogin));
		
		registrationsMan.removeForm(form.getName(), true);
		assertEquals(0, registrationsMan.getRegistrationRequests().size());
	}
	
	@Test
	public void artefactsPresentInFormCantBeRemoved() throws Exception
	{
		initAndCreateForm(false, null);
		
		try
		{
			attrsMan.removeAttributeClass(InitializerCommon.NAMING_AC);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		
		try
		{
			attrsMan.removeAttributeType("cn", true);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		try
		{
			attrsMan.removeAttributeType("email", true);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		
		try
		{
			groupsMan.removeGroup("/B", true);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
	}
	
	@Test
	public void testRequestWithNull() throws EngineException
	{
		initAndCreateForm(true, null);
		RegistrationRequest request = getRequest();
		request.setRegistrationCode(null);
		registrationsMan.submitRegistrationRequest(request, 
				new RegistrationContext(false, false, TriggeringMode.manualAtLogin));
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(request.getRegistrationCode(), fromDb.getRequest().getRegistrationCode());
	}
	
	@Test
	public void testRequests() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, null);
		RegistrationRequest request = getRequest();
		String id1 = registrationsMan.submitRegistrationRequest(request, defContext);
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(request, fromDb.getRequest());
		assertEquals(0, fromDb.getAdminComments().size());
		assertEquals(RegistrationRequestStatus.pending, fromDb.getStatus());
		assertEquals(id1, fromDb.getRequestId());
		assertNotNull(fromDb.getTimestamp());
		
		registrationsMan.processRegistrationRequest(fromDb.getRequestId(), null, 
				RegistrationRequestAction.update, "pub1", "priv1");
		fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(request, fromDb.getRequest());
		assertEquals(2, fromDb.getAdminComments().size());
		assertEquals("priv1", fromDb.getAdminComments().get(1).getContents());
		assertEquals("pub1", fromDb.getAdminComments().get(0).getContents());
		assertEquals(RegistrationRequestStatus.pending, fromDb.getStatus());
		assertEquals(id1, fromDb.getRequestId());
		assertNotNull(fromDb.getTimestamp());

		registrationsMan.processRegistrationRequest(fromDb.getRequestId(), null, 
				RegistrationRequestAction.update, "a2", "p2");
		fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(4, fromDb.getAdminComments().size());
		assertEquals("p2", fromDb.getAdminComments().get(3).getContents());
		assertEquals("a2", fromDb.getAdminComments().get(2).getContents());
		
		registrationsMan.processRegistrationRequest(fromDb.getRequestId(), null, 
				RegistrationRequestAction.drop, null, null);
		assertEquals(0, registrationsMan.getRegistrationRequests().size());
		
		request = getRequest();
		String id2 = registrationsMan.submitRegistrationRequest(request, defContext);
		registrationsMan.processRegistrationRequest(id2, null, 
				RegistrationRequestAction.reject, "a2", "p2");
		fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(request, fromDb.getRequest());
		assertEquals(2, fromDb.getAdminComments().size());
		assertEquals("p2", fromDb.getAdminComments().get(1).getContents());
		assertEquals("a2", fromDb.getAdminComments().get(0).getContents());
		assertEquals(RegistrationRequestStatus.rejected, fromDb.getStatus());
		assertEquals(id2, fromDb.getRequestId());
		assertNotNull(fromDb.getTimestamp());
		
		request = getRequest();
		String id3 = registrationsMan.submitRegistrationRequest(request, defContext);
		registrationsMan.processRegistrationRequest(id3, null, 
				RegistrationRequestAction.accept, "a2", "p2");
		fromDb = registrationsMan.getRegistrationRequests().get(1);
		assertEquals(request, fromDb.getRequest());
		assertEquals(2, fromDb.getAdminComments().size());
		assertEquals("p2", fromDb.getAdminComments().get(1).getContents());
		assertEquals("a2", fromDb.getAdminComments().get(0).getContents());
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
		assertEquals(id3, fromDb.getRequestId());
		assertNotNull(fromDb.getTimestamp());
		
		Entity added = idsMan.getEntity(new EntityParam(new IdentityTaV(X500Identity.ID, "CN=registration test")));
		assertEquals(EntityState.valid, added.getState());
		assertEquals(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT,
				added.getCredentialInfo().getCredentialRequirementId());
		CredentialPublicInformation cpi = added.getCredentialInfo().getCredentialsState().get(
				EngineInitialization.DEFAULT_CREDENTIAL);
		assertEquals(LocalCredentialState.correct, cpi.getState());
		EntityParam addedP = new EntityParam(added.getId());
		Collection<String> groups = idsMan.getGroups(addedP).keySet();
		assertTrue(groups.contains("/"));
		assertTrue(groups.contains("/A"));
		assertTrue(groups.contains("/B"));
		
		Collection<AttributesClass> acs = attrsMan.getEntityAttributeClasses(addedP, "/");
		assertEquals(1, acs.size());
		assertEquals(InitializerCommon.NAMING_AC, acs.iterator().next().getName());
		
		Collection<AttributeExt<?>> attrs = attrsMan.getAttributes(addedP, "/", "cn");
		assertEquals(1, attrs.size());
		assertEquals("val", attrs.iterator().next().getValues().get(0));
		attrs = attrsMan.getAttributes(addedP, "/", "email");
		assertEquals(1, attrs.size());
		VerifiableEmail email = (VerifiableEmail) attrs.iterator().next().getValues().get(0);
		assertEquals("foo@example.com", email.getValue());
		assertEquals(false, email.getConfirmationInfo().isConfirmed());
		
		
		// accept with updates -> check if results are fine
		IdentityParam userIp = new IdentityParam(UsernameIdentity.ID, "some-user");

		request = getRequest();
		IdentityParam ip = new IdentityParam(X500Identity.ID, "CN=registration test2");
		request.setIdentities(Lists.newArrayList(ip, userIp));
		String id4 = registrationsMan.submitRegistrationRequest(request, defContext);
		
		request = getRequest();
		ip = new IdentityParam(X500Identity.ID, "CN=registration test updated");
		request.setIdentities(Lists.newArrayList(ip, userIp));
		registrationsMan.processRegistrationRequest(id4, request, 
				RegistrationRequestAction.accept, "a2", "p2");
		idsMan.getEntity(new EntityParam(new IdentityTaV(X500Identity.ID, "CN=registration test updated")));
		
		
		//TODO test notifications
		
	}

	@Test
	public void formProfileGroupAddingIsRecursive() throws EngineException
	{
		initContents();
		RegistrationContext defContext = new RegistrationContext(true, false, TriggeringMode.manualAtLogin);

		RegistrationTranslationProfileBuilder profileBuilder = new RegistrationTranslationProfileBuilder(
				registry, "form").
				withAutoProcess("true",	AutomaticRequestAction.accept).
				withGroupMembership("true", "'/A/B/C'");
		
		RegistrationFormBuilder formBuilder = getFormBuilder(true, "true", false);
		formBuilder.withTranslationProfile(profileBuilder.build());
		RegistrationForm form = formBuilder.build();
		registrationsMan.addForm(form);
		
		RegistrationRequest request = getRequest();
		request.setRegistrationCode(null);
		registrationsMan.submitRegistrationRequest(request, defContext);
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertThat(fromDb.getStatus(), is(RegistrationRequestStatus.accepted));
		
		Map<String, GroupMembership> groups = idsMan.getGroups(
				new EntityParam(new IdentityTaV(UsernameIdentity.ID, "test-user")));
		assertThat(groups.containsKey("/A/B/C"), is(true));
	}
	
	
	@Test
	public void testRequestsWithAutoAccept() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(true, false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, "true");
		RegistrationRequest request = getRequest();
		registrationsMan.submitRegistrationRequest(request, defContext);
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
		clearDB();
		
		initAndCreateForm(false, "false");
		request = getRequest();
		registrationsMan.submitRegistrationRequest(request, defContext);
		fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.pending, fromDb.getStatus());
		clearDB();
		
		initAndCreateForm(false, "idsByType[\"" + X500Identity.ID +"\"] != null");
		request = getRequest();	
		registrationsMan.submitRegistrationRequest(request, defContext);
		fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
		clearDB();
		
		initAndCreateForm(false, "attr[\"email\"].toString() == \"foo@example.com\"");
		request = getRequest();
		registrationsMan.submitRegistrationRequest(request, defContext);
		fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
		clearDB();
		
		initAndCreateForm(false, "attrs[\"email\"][0] == \"NoAccept\"");
		request = getRequest();
		registrationsMan.submitRegistrationRequest(request, defContext);
		fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.pending, fromDb.getStatus());
		clearDB();
				
		initAndCreateForm(false, "agrs[0] == true");
		request = getRequest();
		registrationsMan.submitRegistrationRequest(request, defContext);
		fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
		clearDB();
		
		initAndCreateForm(false, "agrs[0] == false");
		request = getRequest();
		registrationsMan.submitRegistrationRequest(request, defContext);
		fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.pending, fromDb.getStatus());
		clearDB();		
	}

	
	@Test
	public void requestWithoutOptionalFieldsIsAccepted() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(true, false, TriggeringMode.manualAtLogin);
		initAndCreateForm(true, "true", false);
		RegistrationRequest request = getRequestWithoutOptionalElements();
		String id1 = registrationsMan.submitRegistrationRequest(request, defContext);
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(request, fromDb.getRequest());
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
		assertEquals(id1, fromDb.getRequestId());
		assertNotNull(fromDb.getTimestamp());
	}
	
	private RegistrationFormBuilder getFormBuilder(boolean nullCode, String autoAcceptCondition, boolean addEmailAC)
	{
		RegistrationTranslationProfileBuilder profileBuilder = new RegistrationTranslationProfileBuilder(
				registry, "form").
				withAutoProcess(autoAcceptCondition == null ? "false"
						: autoAcceptCondition, 
						AutomaticRequestAction.accept).
				withAddAttribute("true", "cn", "/", "'val'", AttributeVisibility.full).
				withGroupMembership("true", "'/A'");
		if (addEmailAC)
			profileBuilder.withAttributeClass("true", "/", "'" + InitializerCommon.NAMING_AC + "'");
		
		TranslationProfile translationProfile = profileBuilder.build();
		
		return new RegistrationFormBuilder()
				.withName("f1")
				.withDescription("desc")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withTranslationProfile(translationProfile)
				.withCollectComments(true)
				.withFormInformation(new I18nString("formInformation"))
				.withAddedCredentialParam(
						new CredentialRegistrationParam(EngineInitialization.DEFAULT_CREDENTIAL, null, null))
				.withAddedAgreement(new AgreementRegistrationParam(new I18nString("a"), false))
				.withAddedIdentityParam()
					.withIdentityType(X500Identity.ID)
					.withOptional(true)
					.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedIdentityParam()
					.withIdentityType(UsernameIdentity.ID)
					.withRetrievalSettings(ParameterRetrievalSettings.automaticHidden)
				.endIdentityParam()
				.withAddedAttributeParam()
					.withAttributeType(InitializerCommon.EMAIL_ATTR).withGroup("/")
					.withOptional(true)
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withShowGroups(true).endAttributeParam()
				.withAddedGroupParam()
					.withGroupPath("/B")
					.withRetrievalSettings(ParameterRetrievalSettings.automatic)
				.endGroupParam()
				.withRegistrationCode(nullCode ? null : "123");

	}
	
	private RegistrationRequest getRequest()
	{
		return new RegistrationRequestBuilder()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(
						new VerifiableEmailAttribute(
								InitializerCommon.EMAIL_ATTR, "/",
								AttributeVisibility.full, "foo@example.com"))
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abc").toJson()).endCredential()
				.withAddedGroupSelection().withSelected(true).endGroupSelection()
				.withAddedIdentity()
					.withTypeId(X500Identity.ID)
					.withValue("CN=registration test")
				.endIdentity()
				.withAddedIdentity()
					.withTypeId(UsernameIdentity.ID)
					.withValue("test-user")
				.endIdentity()
				.build();
	}

	private RegistrationRequest getRequestWithoutOptionalElements()
	{
		return new RegistrationRequestBuilder()
				.withFormId("f1")
				.withComments("comments")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedCredential()
				.withCredentialId(EngineInitialization.DEFAULT_CREDENTIAL)
				.withSecrets(new PasswordToken("abc").toJson()).endCredential()
				.withAddedAttribute(null)
				.withAddedIdentity(null)
				.withAddedIdentity()
					.withTypeId(UsernameIdentity.ID)
					.withValue("test-user")
				.endIdentity()
				.withAddedGroupSelection(null)
				.build();
	}
	
	private void checkUpdateOrAdd(RegistrationForm form, String msg, Class<?> exception) throws EngineException
	{
		try
		{
			registrationsMan.addForm(form);
			fail("Added the form with illegal " + msg);
		} catch (EngineException e) 
		{
			assertTrue(e.toString(), e.getClass().isAssignableFrom(exception));
		}
		try
		{
			registrationsMan.updateForm(form, false);
			fail("Updated the form with illegal " + msg);
		} catch (EngineException e) 
		{
			assertTrue(e.toString(), e.getClass().isAssignableFrom(exception));
		}
	}
	private RegistrationForm initAndCreateForm(boolean nullCode, String autoAcceptCondition) throws EngineException
	{
		return initAndCreateForm(nullCode, autoAcceptCondition, true);
	}
	
	private void initContents() throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		groupsMan.addGroup(new Group("/A/B"));
		groupsMan.addGroup(new Group("/A/B/C"));
	}
	
	private RegistrationForm initAndCreateForm(boolean nullCode, String autoAcceptCondition, 
			boolean addEmailAC) throws EngineException
	{
		initContents();
		
		RegistrationForm form = getFormBuilder(nullCode, autoAcceptCondition, addEmailAC).build();

		registrationsMan.addForm(form);
		return form;
	}
	
	private void clearDB() throws EngineException
	{			
		for (RegistrationForm f:registrationsMan.getForms())
			registrationsMan.removeForm(f.getName(), true);
		groupsMan.removeGroup("/A", true);
		groupsMan.removeGroup("/B", true);
		try
		{
			idsMan.removeEntity(new EntityParam(new IdentityTaV(X500Identity.ID, "CN=registration test")));
		} catch (IllegalIdentityValueException e)
		{
			//ok
		}
	}
	
}
