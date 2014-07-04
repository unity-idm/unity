/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.engine.internal.EngineInitialization;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.credential.PasswordToken;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.InitializerCommon;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.registration.AgreementRegistrationParam;
import pl.edu.icm.unity.types.registration.AttributeClassAssignment;
import pl.edu.icm.unity.types.registration.AttributeParamValue;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialParamValue;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.Selection;

public class TestRegistrations extends DBIntegrationTestBase
{
	@Autowired
	private InitializerCommon commonInitializer;
	
	@Test
	public void testRegistrationForms() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false);
		
		List<RegistrationForm> forms = registrationsMan.getForms();
		assertEquals(1, forms.size());
		RegistrationForm read = forms.get(0);
		form.equals(read);
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
		
		
		Attribute<?> attrB = new StringAttribute("missing", "/", AttributeVisibility.full, "val");
		List<Attribute<?>> attrsB = new ArrayList<>();
		attrsB.add(attrB);
		form.setAttributeAssignments(attrsB);
		checkUpdateOrAdd(form, "attr", WrongArgumentException.class);
		form.setAttributeAssignments(null);
		
		AttributeClassAssignment acAB = new AttributeClassAssignment();
		acAB.setAcName("missing");
		acAB.setGroup("/");
		form.setAttributeClassAssignments(Collections.singletonList(acAB));
		checkUpdateOrAdd(form, "AC", WrongArgumentException.class);
		form.setAttributeClassAssignments(null);
		
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
		
		form.setCredentialRequirementAssignment("missing");
		checkUpdateOrAdd(form, "cred req", WrongArgumentException.class);
		form.setCredentialRequirementAssignment(null);
		checkUpdateOrAdd(form, "credential req (2)", WrongArgumentException.class);
		
		form.setCredentialRequirementAssignment(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
		
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
		registrationsMan.submitRegistrationRequest(request);
		assertEquals(1, registrationsMan.getRegistrationRequests().size());
		
		try
		{
			registrationsMan.updateForm(getForm(false), false);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		
		registrationsMan.updateForm(getForm(false), true);
		
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
		initAndCreateForm(true);
		RegistrationRequest request = getRequest();
		request.setRegistrationCode(null);
		registrationsMan.submitRegistrationRequest(request);
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(request.getRegistrationCode(), fromDb.getRequest().getRegistrationCode());
	}
	
	@Test
	public void testRequests() throws EngineException
	{
		initAndCreateForm(false);
		RegistrationRequest request = getRequest();
		String id1 = registrationsMan.submitRegistrationRequest(request);
		
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
		String id2 = registrationsMan.submitRegistrationRequest(request);
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
		String id3 = registrationsMan.submitRegistrationRequest(request);
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
		Collection<String> groups = idsMan.getGroups(addedP);
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
		assertEquals("foo@a.b", attrs.iterator().next().getValues().get(0));
		
		
		// accept with updates -> check if results are fine
		request = getRequest();
		IdentityParam ip = new IdentityParam(X500Identity.ID, "CN=registration test2");
		request.setIdentities(Collections.singletonList(ip));		
		String id4 = registrationsMan.submitRegistrationRequest(request);
		
		request = getRequest();
		ip = new IdentityParam(X500Identity.ID, "CN=registration test updated");
		request.setIdentities(Collections.singletonList(ip));
		registrationsMan.processRegistrationRequest(id4, request, 
				RegistrationRequestAction.accept, "a2", "p2");
		idsMan.getEntity(new EntityParam(new IdentityTaV(X500Identity.ID, "CN=registration test updated")));
		
		
		//TODO test notifications
		
	}
	
	
	private RegistrationForm getForm(boolean nullCode)
	{
		RegistrationForm form = new RegistrationForm();
		
		AgreementRegistrationParam agreement = new AgreementRegistrationParam();
		agreement.setManatory(true);
		agreement.setText("a");
		form.setAgreements(Collections.singletonList(agreement));
		
		Attribute<?> attr = new StringAttribute("cn", "/", AttributeVisibility.full, "val");
		List<Attribute<?>> attrs = new ArrayList<>();
		attrs.add(attr);
		form.setAttributeAssignments(attrs);
		
		AttributeClassAssignment acA = new AttributeClassAssignment();
		acA.setAcName(InitializerCommon.NAMING_AC);
		acA.setGroup("/");
		form.setAttributeClassAssignments(Collections.singletonList(acA));
		
		AttributeRegistrationParam attrReg = new AttributeRegistrationParam();
		attrReg.setAttributeType("email");
		attrReg.setDescription("description");
		attrReg.setGroup("/");
		attrReg.setLabel("label");
		attrReg.setOptional(true);
		attrReg.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		attrReg.setShowGroups(true);
		attrReg.setUseDescription(true);
		form.setAttributeParams(Collections.singletonList(attrReg));
		
		form.setCollectComments(true);
		
		CredentialRegistrationParam credParam = new CredentialRegistrationParam();
		credParam.setCredentialName(EngineInitialization.DEFAULT_CREDENTIAL);
		credParam.setDescription("description");
		credParam.setLabel("label");
		form.setCredentialParams(Collections.singletonList(credParam));
		
		
		form.setCredentialRequirementAssignment(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
		form.setDescription("description");
		form.setFormInformation("formInformation");
		form.setGroupAssignments(Collections.singletonList("/A"));
		
		GroupRegistrationParam groupParam = new GroupRegistrationParam();
		groupParam.setDescription("description");
		groupParam.setGroupPath("/B");
		groupParam.setLabel("label");
		groupParam.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		form.setGroupParams(Collections.singletonList(groupParam));
		
		IdentityRegistrationParam idParam = new IdentityRegistrationParam();
		idParam.setDescription("description");
		idParam.setIdentityType(X500Identity.ID);
		idParam.setLabel("label");
		idParam.setOptional(true);
		idParam.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden);
		form.setIdentityParams(Collections.singletonList(idParam));
		form.setName("f1");
		form.setPubliclyAvailable(true);
		if (!nullCode)
			form.setRegistrationCode("123");
		form.setInitialEntityState(EntityState.valid);
		return form;
	}
	
	private RegistrationRequest getRequest()
	{
		RegistrationRequest request = new RegistrationRequest();
		
		request.setAgreements(Collections.singletonList(new Selection(true)));
		AttributeParamValue ap = new AttributeParamValue();
		ap.setAttribute(new StringAttribute("email", "/", AttributeVisibility.full, "foo@a.b"));
		request.setAttributes(Collections.singletonList(ap));
		request.setComments("comments");
		CredentialParamValue cp = new CredentialParamValue();
		cp.setCredentialId(EngineInitialization.DEFAULT_CREDENTIAL);
		cp.setSecrets(new PasswordToken("abc").toJson());
		request.setCredentials(Collections.singletonList(cp));
		request.setFormId("f1");
		request.setGroupSelections(Collections.singletonList(new Selection(true)));
		IdentityParam ip = new IdentityParam(X500Identity.ID, "CN=registration test");
		request.setIdentities(Collections.singletonList(ip));
		request.setRegistrationCode("123");
		return request;
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
	
	private RegistrationForm initAndCreateForm(boolean nullCode) throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		commonInitializer.initializeMainAttributeClass();
		groupsMan.addGroup(new Group("/A"));
		groupsMan.addGroup(new Group("/B"));
		
		RegistrationForm form = getForm(nullCode);

		registrationsMan.addForm(form);
		return form;
	}
}
