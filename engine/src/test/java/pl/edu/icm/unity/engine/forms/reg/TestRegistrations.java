/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.SchemaConsistencyException;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.identity.X500Identity;
import pl.edu.icm.unity.stdext.utils.EmailUtils;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.EntityState;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.VerifiableEmail;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

public class TestRegistrations extends RegistrationTestBase
{
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
		} catch (IllegalArgumentException e) {/*ok*/}
	}
	
	@Test 
	public void formWithDuplicateNameCantBeAdded() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		try
		{
			registrationsMan.addForm(form);
			fail("Added the same form twice");
		} catch (IllegalArgumentException e) {/*ok*/}
	}
	
	@Test 
	public void formWithMissingAttributeCantBeAdded() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		RegistrationFormBuilder testFormBuilder = getFormBuilder(false, null, true);

		AttributeRegistrationParam attrReg = form.getAttributeParams().get(0);
		attrReg.setAttributeType("missing");
		testFormBuilder.withAttributeParams(Collections.singletonList(attrReg));
		
		checkUpdateOrAdd(testFormBuilder.build(), "attr(2)", IllegalArgumentException.class);
	}
	
	@Test 
	public void formWithMissingCredentialCantBeAdded() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		RegistrationFormBuilder testFormBuilder = getFormBuilder(false, null, true);

		CredentialRegistrationParam credParam = form.getCredentialParams().get(0);
		credParam.setCredentialName("missing");
		testFormBuilder.withCredentialParams(Collections.singletonList(credParam));
		
		checkUpdateOrAdd(testFormBuilder.build(), "cred", IllegalArgumentException.class);
	}

	
	@Test 
	public void formWithMissingCredentialReqCantBeAdded() throws Exception
	{
		initAndCreateForm(false, null);
		RegistrationFormBuilder testFormBuilder = getFormBuilder(false, null, true);
		testFormBuilder.withDefaultCredentialRequirement("missing");
		checkUpdateOrAdd(testFormBuilder.build(), "cred req", IllegalArgumentException.class);
	}
	
	@Test 
	public void formWithMissingIdentityCantBeAdded() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		RegistrationFormBuilder testFormBuilder = getFormBuilder(false, null, true);
		IdentityRegistrationParam idParam = form.getIdentityParams().get(0);
		idParam.setIdentityType("missing");
		testFormBuilder.withIdentityParams(Collections.singletonList(idParam));
		checkUpdateOrAdd(testFormBuilder.build(), "id", IllegalArgumentException.class);
	}

	@Test
	public void formWithRequestCantBeUpdated() throws Exception
	{
		initAndCreateForm(false, null);
		
		registrationsMan.submitRegistrationRequest(getRequest(), 
				new RegistrationContext(false, TriggeringMode.manualAtLogin));
		
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
				new RegistrationContext(false, TriggeringMode.manualAtLogin));
		
		registrationsMan.updateForm(getFormBuilder(false, null, true).build(), true);
	}

	@Test
	public void formWithRequestCantBeRemoved() throws Exception
	{
		RegistrationForm form = initAndCreateForm(false, null);
		
		registrationsMan.submitRegistrationRequest(getRequest(), 
				new RegistrationContext(false, TriggeringMode.manualAtLogin));
		
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
				new RegistrationContext(false, TriggeringMode.manualAtLogin));
		
		registrationsMan.removeForm(form.getName(), true);
		assertEquals(0, registrationsMan.getRegistrationRequests().size());
	}
	
	@Test
	public void artefactsPresentInFormCantBeRemoved() throws Exception
	{
		initAndCreateForm(false, null);
		
		try
		{
			acMan.removeAttributeClass(InitializerCommon.NAMING_AC);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		
		try
		{
			aTypeMan.removeAttributeType("cn", true);
		} catch (SchemaConsistencyException e)
		{
			//OK
		}
		try
		{
			aTypeMan.removeAttributeType("email", true);
		} catch (IllegalArgumentException e)
		{
			//OK
		}
		
		try
		{
			groupsMan.removeGroup("/B", true);
		} catch (IllegalArgumentException e)
		{
			//OK
		}
	}
	
	@Test
	public void requestWithNullCodeIsAcceptedForFormWithoutCode() throws EngineException
	{
		initAndCreateForm(true, null);
		RegistrationRequest request = getRequest();
		request.setRegistrationCode(null);
		registrationsMan.submitRegistrationRequest(request, 
				new RegistrationContext(false, TriggeringMode.manualAtLogin));
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertThat(fromDb.getRequest().getRegistrationCode(), is(nullValue()));
	}
	
	
	@Test
	public void addedCommentsAreReturned() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, null);
		RegistrationRequest request = getRequest();
		String id = registrationsMan.submitRegistrationRequest(request, defContext);
		
		registrationsMan.processRegistrationRequest(id, null, 
				RegistrationRequestAction.update, "pub1", "priv1");
		registrationsMan.processRegistrationRequest(id, null, 
				RegistrationRequestAction.update, "a2", "p2");

		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(4, fromDb.getAdminComments().size());
		assertEquals("pub1", fromDb.getAdminComments().get(0).getContents());
		assertThat(fromDb.getAdminComments().get(0).isPublicComment(), is(true));
		assertEquals("priv1", fromDb.getAdminComments().get(1).getContents());
		assertThat(fromDb.getAdminComments().get(1).isPublicComment(), is(false));
		assertEquals("a2", fromDb.getAdminComments().get(2).getContents());
		assertThat(fromDb.getAdminComments().get(2).isPublicComment(), is(true));
		assertEquals("p2", fromDb.getAdminComments().get(3).getContents());
		assertThat(fromDb.getAdminComments().get(3).isPublicComment(), is(false));
	}
	
	@Test
	public void addedRequestIsReturned() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, null);
		RegistrationRequest request = getRequest();
		String id1 = registrationsMan.submitRegistrationRequest(request, defContext);
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertThat(fromDb.getAdminComments().isEmpty(), is(true));
		assertThat(fromDb.getRequest(), is(request));
		assertEquals(0, fromDb.getAdminComments().size());
		assertEquals(RegistrationRequestStatus.pending, fromDb.getStatus());
		assertEquals(id1, fromDb.getRequestId());
		assertNotNull(fromDb.getTimestamp());
	}
	
	@Test
	public void droppedRequestIsNotReturned() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, null);
		RegistrationRequest request = getRequest();
		String id = registrationsMan.submitRegistrationRequest(request, defContext);
		
		registrationsMan.processRegistrationRequest(id, null, 
				RegistrationRequestAction.drop, null, null);
		assertEquals(0, registrationsMan.getRegistrationRequests().size());
	}
	
	@Test
	public void rejectedRequestIsReturned() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, null);

		RegistrationRequest request = getRequest();
		String id2 = registrationsMan.submitRegistrationRequest(request, defContext);
		registrationsMan.processRegistrationRequest(id2, null, 
				RegistrationRequestAction.reject, "a2", "p2");
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(request, fromDb.getRequest());
		assertEquals(2, fromDb.getAdminComments().size());
		assertEquals("p2", fromDb.getAdminComments().get(1).getContents());
		assertEquals("a2", fromDb.getAdminComments().get(0).getContents());
		assertEquals(RegistrationRequestStatus.rejected, fromDb.getStatus());
		assertEquals(id2, fromDb.getRequestId());
		assertNotNull(fromDb.getTimestamp());
	}
	
	@Test
	public void acceptedRequestIsFullyApplied() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, null);
		RegistrationRequest request = getRequest();
		String id3 = registrationsMan.submitRegistrationRequest(request, defContext);

		
		registrationsMan.processRegistrationRequest(id3, null, 
				RegistrationRequestAction.accept, "a2", "p2");
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
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
		assertThat(fromDb.getCreatedEntityId(), is(added.getId()));
		
		CredentialPublicInformation cpi = added.getCredentialInfo().getCredentialsState().get(
				EngineInitialization.DEFAULT_CREDENTIAL);
		assertEquals(LocalCredentialState.correct, cpi.getState());
		EntityParam addedP = new EntityParam(added.getId());
		Collection<String> groups = idsMan.getGroups(addedP).keySet();
		assertTrue(groups.contains("/"));
		assertTrue(groups.contains("/A"));
		assertTrue(groups.contains("/B"));
		
		Collection<AttributesClass> acs = acMan.getEntityAttributeClasses(addedP, "/");
		assertEquals(1, acs.size());
		assertEquals(InitializerCommon.NAMING_AC, acs.iterator().next().getName());
		
		Collection<AttributeExt> attrs = attrsMan.getAttributes(addedP, "/", "cn");
		assertEquals(1, attrs.size());
		assertEquals("val", attrs.iterator().next().getValues().get(0));
		attrs = attrsMan.getAttributes(addedP, "/", "email");
		assertEquals(1, attrs.size());
		
		String value = attrs.iterator().next().getValues().get(0);
		VerifiableEmail ve = new VerifiableEmail(JsonUtil.parse(value)); //FIXME - this is likely wrong
		
		assertEquals("foo@example.com", ve.getValue());
		assertEquals(false, ve.getConfirmationInfo().isConfirmed());
	}	
	
	@Test
	public void updateOfRequestIdentityUponAcceptIsRespected() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, null);
		
		RegistrationRequest request = getRequest();
		IdentityParam userIp = new IdentityParam(UsernameIdentity.ID, "some-user");
		IdentityParam ip = new IdentityParam(X500Identity.ID, "CN=registration test2");
		request.setIdentities(Lists.newArrayList(ip, userIp));
		String id4 = registrationsMan.submitRegistrationRequest(request, defContext);
		
		request = getRequest();
		IdentityParam changed = new IdentityParam(X500Identity.ID, "CN=registration test updated");
		request.setIdentities(Lists.newArrayList(changed, userIp));
		registrationsMan.processRegistrationRequest(id4, request, 
				RegistrationRequestAction.accept, "a2", "p2");
		idsMan.getEntity(new EntityParam(new IdentityTaV(X500Identity.ID, "CN=registration test updated")));
	}

	@Test
	public void formProfileGroupAddingIsRecursive() throws EngineException
	{
		initContents();
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);

		TranslationAction a1 = new TranslationAction(AutoProcessActionFactory.NAME, 
				new String[] {AutomaticRequestAction.accept.toString()});
		TranslationAction a2 = new TranslationAction(AddToGroupActionFactory.NAME, 
				new String[] {"'/A/B/C'"});
		TranslationProfile tp = new TranslationProfile("form", "", ProfileType.REGISTRATION, 
				Lists.newArrayList(new TranslationRule("true", a1),
						new TranslationRule("true", a2)));
		
		RegistrationFormBuilder formBuilder = getFormBuilder(true, "true", false);
		formBuilder.withTranslationProfile(tp);
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
	public void requestWithAutoAcceptIsAccepted() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, "true");
		RegistrationRequest request = getRequest();
		
		registrationsMan.submitRegistrationRequest(request, defContext);
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
	}

	@Test
	public void requestWithoutAutoAcceptIsPending() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, "false");
		RegistrationRequest request = getRequest();
		
		registrationsMan.submitRegistrationRequest(request, defContext);
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.pending, fromDb.getStatus());
	}

	@Test
	public void requestWithMetAutoAcceptConditionIsAccepted() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, "idsByType[\"" + X500Identity.ID +"\"] != null");
		RegistrationRequest request = getRequest();
		
		registrationsMan.submitRegistrationRequest(request, defContext);
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
	}

	@Test
	public void requestWithMetAutoAcceptConditionIsAccepted2() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, "attr[\"email\"].toString() == \"foo@example.com\"");
		RegistrationRequest request = getRequest();
		
		registrationsMan.submitRegistrationRequest(request, defContext);
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
	}

	@Test
	public void requestWithNotMetAutoAcceptConditionIsPending() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, "attrs[\"email\"][0] == \"NoAccept\"");
		RegistrationRequest request = getRequest();
		
		registrationsMan.submitRegistrationRequest(request, defContext);
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.pending, fromDb.getStatus());
	}

	@Test
	public void requestWithMetAutoAcceptConditionIsAccepted3() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);	
		initAndCreateForm(false, "agrs[0] == true");
		RegistrationRequest request = getRequest();
		
		registrationsMan.submitRegistrationRequest(request, defContext);
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
	}

	@Test
	public void requestWithNotMetAutoAcceptConditionIsPending2() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(false, "agrs[0] == false");
		RegistrationRequest request = getRequest();
		
		registrationsMan.submitRegistrationRequest(request, defContext);

		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(RegistrationRequestStatus.pending, fromDb.getStatus());
	}

	
	@Test
	public void requestWithoutOptionalFieldsIsAccepted() throws EngineException
	{
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.manualAtLogin);
		initAndCreateForm(true, "true", false);
		RegistrationRequest request = getRequestWithoutOptionalElements();
		String id1 = registrationsMan.submitRegistrationRequest(request, defContext);
		
		RegistrationRequestState fromDb = registrationsMan.getRegistrationRequests().get(0);
		assertEquals(request, fromDb.getRequest());
		assertEquals(RegistrationRequestStatus.accepted, fromDb.getStatus());
		assertEquals(id1, fromDb.getRequestId());
		assertNotNull(fromDb.getTimestamp());
	}
	
	@Test
	public void submittedRequestAfterRemoteAuthnFromRegistrationDoesNotValidateCredentials() throws EngineException
	{
		// given
		RegistrationContext defContext = new RegistrationContext(false, TriggeringMode.afterRemoteLoginFromRegistrationForm);
		initAndCreateForm(false, null);
		Attribute emailA = new Attribute(InitializerCommon.EMAIL_ATTR, VerifiableEmailAttributeSyntax.ID, "/",
				Lists.newArrayList(EmailUtils.convertFromString("foo@example.com").toJsonString()));
		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId("f1")
				.withComments("comments")
				.withRegistrationCode("123")
				.withAddedAgreement()
				.withSelected(true)
				.endAgreement()
				.withAddedAttribute(emailA)
				.withAddedGroupSelection().withGroup("/B").endGroupSelection()
				.withAddedIdentity(new IdentityParam(X500Identity.ID, "CN=registration test"))
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "test-user"))
				.build();
		
		// when
		Throwable exception = Assertions.catchThrowable(() -> registrationsMan.submitRegistrationRequest(request, defContext));
		
		// then
		Assertions.assertThat(exception).isNull();
	}
	
}
