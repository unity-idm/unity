/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.builders.RegistrationFormBuilder;
import pl.edu.icm.unity.engine.builders.RegistrationRequestBuilder;
import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.RegistrationContext;
import pl.edu.icm.unity.server.api.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.server.registries.RegistrationActionsRegistry;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfileBuilder;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.stdext.attr.VerifiableEmail;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;

public class TestRegistrations extends DBIntegrationTestBase
{
	@Autowired
	private InitializerCommon commonInitializer;
	
	@Autowired
	private RegistrationActionsRegistry registry;
	
	@Test
	public void testRegistrationForms() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		
		List<RegistrationForm> forms = registrationsMan.getForms();
		assertEquals(1, forms.size());
		assertEquals(form, forms.get(0));
		
		registrationsMan.removeForm("f1", false);
		assertEquals(0, registrationsMan.getForms().size());
		try
		{
			registrationsMan.removeForm("f1", true);
			fail("Removed the same form twice");
		} catch (WrongArgumentException e) {/*ok*/}
		
		registrationsMan.addForm(form);
		
		try
		{
			registrationsMan.addForm(form);
			fail("Added the same form twice");
		} catch (WrongArgumentException e) {/*ok*/}
		
		
		AttributeRegistrationParam attrReg = form.getAttributeParams().get(0);
		attrReg.setAttributeType("missing");
		form.setAttributeParams(Collections.singletonList(attrReg));
		checkUpdateOrAdd(form, "attr(2)", WrongArgumentException.class);
		form.setAttributeParams(null);
		
		CredentialRegistrationParam credParam = form.getCredentialParams().get(0);
		credParam.setCredentialName("missing");
		form.setCredentialParams(Collections.singletonList(credParam));
		checkUpdateOrAdd(form, "cred", WrongArgumentException.class);
		form.setCredentialParams(null);
		
		form.setDefaultCredentialRequirement("missing");
		checkUpdateOrAdd(form, "cred req", WrongArgumentException.class);
		form.setDefaultCredentialRequirement(null);
		checkUpdateOrAdd(form, "credential req (2)", WrongArgumentException.class);
		
		form.setDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
		
		GroupRegistrationParam groupParam = form.getGroupParams().get(0);
		groupParam.setGroupPath("/missing");
		form.setGroupParams(Collections.singletonList(groupParam));
		checkUpdateOrAdd(form, "group", IllegalGroupValueException.class);
		form.setGroupParams(null);
		
		IdentityRegistrationParam idParam = form.getIdentityParams().get(0);
		idParam.setIdentityType("missing");
		form.setIdentityParams(Collections.singletonList(idParam));
		checkUpdateOrAdd(form, "id", IllegalTypeException.class);
		idParam.setIdentityType(UsernameIdentity.ID);
		form.setIdentityParams(Collections.singletonList(idParam));
		
		RegistrationRequest request = getRequest();
		registrationsMan.submitRegistrationRequest(request, 
				new RegistrationContext(false, false, TriggeringMode.manualAtLogin));
		assertEquals(1, registrationsMan.getRegistrationRequests().size());
		
		try
		{
			registrationsMan.updateForm(getForm(false, null), false);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		
		registrationsMan.updateForm(getForm(false, null), true);
		
		try
		{
			registrationsMan.removeForm(form.getName(), false);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		registrationsMan.removeForm(form.getName(), true);
		
		assertEquals(0, registrationsMan.getRegistrationRequests().size());
		
		
		//test consistency
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
		request = getRequest();
		IdentityParam ip = new IdentityParam(X500Identity.ID, "CN=registration test2");
		request.setIdentities(Collections.singletonList(ip));		
		String id4 = registrationsMan.submitRegistrationRequest(request, defContext);
		
		request = getRequest();
		ip = new IdentityParam(X500Identity.ID, "CN=registration test updated");
		request.setIdentities(Collections.singletonList(ip));
		registrationsMan.processRegistrationRequest(id4, request, 
				RegistrationRequestAction.accept, "a2", "p2");
		idsMan.getEntity(new EntityParam(new IdentityTaV(X500Identity.ID, "CN=registration test updated")));
		
		
		//TODO test notifications
		
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
		
	private RegistrationForm getForm(boolean nullCode, String autoAcceptCondition)
	{
		RegistrationTranslationProfile translationProfile = new RegistrationTranslationProfileBuilder(
				registry, "form").
				withAutoProcess(autoAcceptCondition == null ? "false"
						: autoAcceptCondition, 
						AutomaticRequestAction.accept).
				withAddAttribute("true", "cn", "/", "'val'", AttributeVisibility.full).
				withGroupMembership("true", "'/A'").
				withAttributeClass("true", "/", "'" + InitializerCommon.NAMING_AC + "'").
				build();
		return RegistrationFormBuilder
				.registrationForm()
				.withName("f1")
				.withDescription("desc")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withTranslationProfile(translationProfile)
				.withCollectComments(true).withFormInformation()
				.withDefaultValue("formInformation").endFormInformation()
				.withAddedCredentialParam()
				.withCredentialName(EngineInitialization.DEFAULT_CREDENTIAL)
				.endCredentialParam()
				.withAddedAgreement().withManatory(false).withText()
				.withDefaultValue("a").endText().endAgreement()
				.withAddedIdentityParam()
				.withIdentityType(X500Identity.ID)
				.withOptional(true)
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
				.withRegistrationCode(nullCode ? null : "123")
				.build();

	}
	
	private RegistrationRequest getRequest()
	{
		return RegistrationRequestBuilder
				.registrationRequest()
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
				.withAddedIdentity().withTypeId(X500Identity.ID)
				.withValue("CN=registration test").endIdentity()
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
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		
		RegistrationForm form = getForm(nullCode, autoAcceptCondition);

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
			idsMan.removeIdentity(new IdentityTaV(X500Identity.ID, "CN=registration test"));
		} catch (IllegalIdentityValueException e)
		{
			//ok
		}
	}
	
}
