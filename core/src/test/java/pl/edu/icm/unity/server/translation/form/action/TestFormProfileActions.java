/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.MockAttributeSyntax;
import pl.edu.icm.unity.server.api.internal.AttributesInternalProcessing;
import pl.edu.icm.unity.server.translation.form.GroupParam;
import pl.edu.icm.unity.server.translation.form.RegistrationMVELContext;
import pl.edu.icm.unity.server.translation.form.RegistrationMVELContext.ContextKey;
import pl.edu.icm.unity.server.translation.form.RegistrationMVELContext.RequestSubmitStatus;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.server.translation.form.action.SetEntityStateActionFactory.EntityStateLimited;
import pl.edu.icm.unity.types.EntityScheduledOperation;
import pl.edu.icm.unity.types.EntityState;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.IdentityRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.Selection;

import com.google.common.collect.Lists;

public class TestFormProfileActions
{
	@Test
	public void testAddAttribute() throws EngineException
	{
		AttributesInternalProcessing attrsMan = mock(AttributesInternalProcessing.class);
		
		Map<String, AttributeType> mockAts = new HashMap<String, AttributeType>();
		AttributeType sA = new AttributeType("stringA", new MockAttributeSyntax());
		mockAts.put(sA.getName(), sA);
		when(attrsMan.getAttributeTypesAsMap()).thenReturn(mockAts);
		
		AddAttributeActionFactory factory = new AddAttributeActionFactory(attrsMan);
		
		RegistrationTranslationAction action = factory.getInstance("stringA", "/A/B", 
				"attr['attribute']", 
				AttributeVisibility.full.toString());
				
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getAttributes().size(), is(1));
		Attribute<?> a = state.getAttributes().iterator().next();
		assertThat(a.getName(), is("stringA"));
		assertThat(a.getValues().get(0), is("a1"));
	}

	@Test
	public void testFilterAttribute() throws EngineException
	{
		FilterAttributeActionFactory factory = new FilterAttributeActionFactory();
		
		RegistrationTranslationAction action = factory.getInstance("a.*", "/");
				
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		state.addAttribute(new Attribute<>("attribute", 
				new MockAttributeSyntax(), "/", AttributeVisibility.full, 
				Lists.newArrayList("a1")));
		state.addAttribute(new Attribute<>("other", 
				new MockAttributeSyntax(), "/", AttributeVisibility.full, 
				Lists.newArrayList("a2"))); 
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getAttributes().size(), is(1));
		Attribute<?> a = state.getAttributes().iterator().next();
		assertThat(a.getName(), is("other"));
		assertThat(a.getValues().get(0), is("a2"));
	}
	
	@Test
	public void testAddGroup() throws EngineException
	{
		AddToGroupActionFactory factory = new AddToGroupActionFactory();
		
		RegistrationTranslationAction action = factory.getInstance("'/A'");
				
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getGroups().size(), is(1));
		GroupParam g = state.getGroups().iterator().next();
		assertThat(g.getGroup(), is("/A"));
	}

	@Test
	public void testFilterGroup() throws EngineException
	{
		FilterGroupActionFactory factory = new FilterGroupActionFactory();
		
		RegistrationTranslationAction action = factory.getInstance("/A.*");
				
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		state.addMembership(new GroupParam("/A/B", null, null));
		state.addMembership(new GroupParam("/Z", null, null));
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getGroups().size(), is(1));
		GroupParam a = state.getGroups().iterator().next();
		assertThat(a.getGroup(), is("/Z"));
	}

	@Test
	public void testFilterIdentity() throws EngineException
	{
		FilterIdentityActionFactory factory = new FilterIdentityActionFactory();
		
		RegistrationTranslationAction action = factory.getInstance("id.*", "idT");
				
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		state.addIdentity(new IdentityParam("idT", "idAA"));
		state.addIdentity(new IdentityParam("idT", "bbb"));
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getIdentities().size(), is(1));
		IdentityParam a = state.getIdentities().iterator().next();
		assertThat(a.getValue(), is("bbb"));
	}
	
	@Test
	public void testAddIdentity() throws EngineException
	{
		AddIdentityActionFactory factory = new AddIdentityActionFactory();
		
		RegistrationTranslationAction action = factory.getInstance("idType", "'identity'");
				
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getIdentities().size(), is(1));
		IdentityParam id = state.getIdentities().iterator().next();
		assertThat(id.getValue(), is("identity"));
		assertThat(id.getTypeId(), is("idType"));
	}

	@Test
	public void testAddAC() throws EngineException
	{
		AddAttributeClassActionFactory factory = new AddAttributeClassActionFactory();
		
		RegistrationTranslationAction action = factory.getInstance("/A", "'ac'");
				
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		
		action.invoke(state, createContext(), "testProf");
		
		Map<String, Set<String>> attributeClasses = state.getAttributeClasses();
		assertThat(attributeClasses.size(), is(1));
		assertThat(attributeClasses.get("/A").size(), is(1));
		assertThat(attributeClasses.get("/A").iterator().next(), is("ac"));
	}
	
	@Test
	public void testAutoProcess() throws EngineException
	{
		AutoProcessActionFactory factory = new AutoProcessActionFactory();
		RegistrationTranslationAction action = factory.getInstance(AutomaticRequestAction.accept.toString());
		
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getAutoAction(), is(AutomaticRequestAction.accept));
	}	

	@Test
	public void testConfirmationRedirect() throws EngineException
	{
		ConfirmationRedirectActionFactory factory = new ConfirmationRedirectActionFactory();
		RegistrationTranslationAction action = factory.getInstance("'http://someurl'");
		
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getRedirectURL(), is("http://someurl"));
	}
	
	@Test
	public void testRedirect() throws EngineException
	{
		RedirectActionFactory factory = new RedirectActionFactory();
		RegistrationTranslationAction action = factory.getInstance("'http://someurl'");
		
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getRedirectURL(), is("http://someurl"));
	}	

	@Test
	public void testScheduleChange() throws EngineException
	{
		ScheduleEntityChangeActionFactory factory = new ScheduleEntityChangeActionFactory();
		RegistrationTranslationAction action = factory.getInstance(
				EntityScheduledOperation.REMOVE.toString(), "4");
		
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getEntityChange().getScheduledOperation(), is(EntityScheduledOperation.REMOVE));
		LocalDate expected = LocalDate.now();
		expected = expected.plus(4, ChronoUnit.DAYS);
		LocalDate received = state.getEntityChange().getScheduledTime().toInstant().atZone(
				ZoneId.systemDefault()).toLocalDate();
		assertThat(received, is(expected));
	}
	
	@Test
	public void testSetCR() throws EngineException
	{
		SetCredentialRequirementActionFactory factory = new SetCredentialRequirementActionFactory();
		RegistrationTranslationAction action = factory.getInstance("credReq");
		
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getCredentialRequirement(), is("credReq"));
	}
	
	@Test
	public void testSetState() throws EngineException
	{
		SetEntityStateActionFactory factory = new SetEntityStateActionFactory();
		RegistrationTranslationAction action = factory.getInstance(EntityStateLimited.disabled.toString());
		
		TranslatedRegistrationRequest state = new TranslatedRegistrationRequest("defaultCR");
		
		action.invoke(state, createContext(), "testProf");
		
		assertThat(state.getEntityState(), is(EntityState.disabled));
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testContext()
	{
		Map<String, Object> context = createContext();
		
		assertThat(((Map<String, Object>)context.get("attr")).containsKey("attribute"), is(true));
		assertThat(((Map<String, Object>)context.get("attrs")).containsKey("attribute"), is(true));
		assertThat(((Map<String, Object>)context.get("attr")).containsKey("other"), is(true));
		assertThat(((Map<String, Object>)context.get("attrs")).containsKey("other"), is(true));

		assertThat(((Map<String, Object>)context.get("rattr")).containsKey("attribute"), is(false));
		assertThat(((Map<String, Object>)context.get("rattrs")).containsKey("attribute"), is(false));
		assertThat(((Map<String, Object>)context.get("rattr")).containsKey("other"), is(true));
		assertThat(((Map<String, Object>)context.get("rattrs")).containsKey("other"), is(true));

		assertThat(((Map<String, Object>)context.get("rattr")).containsKey("attribute"), is(false));
		assertThat(((Map<String, Object>)context.get("rattrs")).containsKey("attribute"), is(false));
		assertThat(((Map<String, Object>)context.get("rattr")).containsKey("other"), is(true));
		assertThat(((Map<String, Object>)context.get("rattrs")).containsKey("other"), is(true));

		assertThat(((Map<String, List<String>>)context.get("idsByType")).containsKey("identifier"), is(true));
		assertThat(((Map<String, List<Object>>)context.get("idsByTypeObj")).containsKey("identifier"), is(true));
		assertThat(((Map<String, List<String>>)context.get("idsByType")).containsKey("username"), is(true));
		assertThat(((Map<String, List<Object>>)context.get("idsByTypeObj")).containsKey("username"), is(true));

		assertThat(((Map<String, List<String>>)context.get("idsByType")).get("identifier").size(), is(1));
		assertThat(((Map<String, List<Object>>)context.get("idsByTypeObj")).get("identifier").size(), is(1));
		assertThat(((Map<String, List<String>>)context.get("idsByType")).get("identifier").get(0), is("id"));
		assertThat(((Map<String, List<Object>>)context.get("idsByTypeObj")).get("identifier").get(0), is("id"));

		assertThat(((Map<String, List<String>>)context.get("idsByType")).get("username").size(), is(1));
		assertThat(((Map<String, List<Object>>)context.get("idsByTypeObj")).get("username").size(), is(1));
		assertThat(((Map<String, List<String>>)context.get("idsByType")).get("username").get(0), is("user"));
		assertThat(((Map<String, List<Object>>)context.get("idsByTypeObj")).get("username").get(0), is("user"));

		assertThat(((Map<String, List<String>>)context.get("ridsByType")).containsKey("identifier"), is(false));
		assertThat(((Map<String, List<Object>>)context.get("ridsByTypeObj")).containsKey("identifier"), is(false));
		assertThat(((Map<String, List<String>>)context.get("ridsByType")).containsKey("username"), is(true));
		assertThat(((Map<String, List<Object>>)context.get("ridsByTypeObj")).containsKey("username"), is(true));

		assertThat(((Map<String, List<String>>)context.get("ridsByType")).get("username").size(), is(1));
		assertThat(((Map<String, List<Object>>)context.get("ridsByTypeObj")).get("username").size(), is(1));
		assertThat(((Map<String, List<String>>)context.get("ridsByType")).get("username").get(0), is("user"));
		assertThat(((Map<String, List<Object>>)context.get("ridsByTypeObj")).get("username").get(0), is("user"));
		
		assertThat(((List<String>)context.get("groups")).size(), is(2));
		assertThat(((List<String>)context.get("rgroups")).size(), is(1));
		assertThat(((List<String>)context.get("groups")).contains("/local"), is(true));
		assertThat(((List<String>)context.get("groups")).contains("/remote"), is(true));
		assertThat(((List<String>)context.get("rgroups")).contains("/local"), is(false));
		assertThat(((List<String>)context.get("rgroups")).contains("/remote"), is(true));
		
		assertThat(((List<String>)context.get("agrs")).size(), is(1));
		assertThat(((List<String>)context.get("agrs")).get(0), is("true"));
		
		assertThat(((String)context.get(ContextKey.userLocale.name())), is("en"));
		assertThat(((String)context.get(ContextKey.requestId.name())), is("requestId"));
		assertThat(((Boolean)context.get(ContextKey.onIdpEndpoint.name())), is(false));
		assertThat(((String)context.get(ContextKey.triggered.name())), 
				is(TriggeringMode.manualAtLogin.toString()));
		assertThat(((String)context.get(ContextKey.status.name())), 
				is(RequestSubmitStatus.submitted.toString()));
		assertThat(((String)context.get(ContextKey.registrationForm.name())), is("form"));
	}
	
	private Map<String, Object> createContext()
	{
		RegistrationRequest request = mock(RegistrationRequest.class);
		when(request.getAttributes()).thenReturn(Lists.newArrayList(
				new Attribute<>("attribute", 
						new MockAttributeSyntax(), "/", AttributeVisibility.full, 
						Lists.newArrayList("a1")),
				new Attribute<>("other", 
						new MockAttributeSyntax(), "/", AttributeVisibility.full, 
						Lists.newArrayList("a2")) 
					));
		when(request.getIdentities()).thenReturn(Lists.newArrayList(
				new IdentityParam("identifier", "id"),
				new IdentityParam("username", "user", "idp", "prof")
				));
		when(request.getGroupSelections()).thenReturn(Lists.newArrayList(
				new Selection(true),
				new Selection(true, "idp", "prof")
				));
		when(request.getAgreements()).thenReturn(Lists.newArrayList(
				new Selection(true)
				));
		when(request.getUserLocale()).thenReturn("en");
		
		RegistrationForm form = mock(RegistrationForm.class);
		AttributeRegistrationParam interactiveARP = new AttributeRegistrationParam();
		interactiveARP.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		AttributeRegistrationParam automaticARP = new AttributeRegistrationParam();
		automaticARP.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden);
		when(form.getAttributeParams()).thenReturn(Lists.newArrayList(
				interactiveARP,
				automaticARP
				));
		
		IdentityRegistrationParam interactiveIRP = new IdentityRegistrationParam();
		interactiveIRP.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		IdentityRegistrationParam automaticIRP = new IdentityRegistrationParam();
		automaticIRP.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		when(form.getIdentityParams()).thenReturn(Lists.newArrayList(
				interactiveIRP,
				automaticIRP
				));
		
		GroupRegistrationParam interactiveGRP = new GroupRegistrationParam();
		interactiveGRP.setGroupPath("/local");
		interactiveGRP.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		GroupRegistrationParam automaticGRP = new GroupRegistrationParam();
		automaticGRP.setGroupPath("/remote");
		automaticGRP.setRetrievalSettings(ParameterRetrievalSettings.automatic);
		when(form.getGroupParams()).thenReturn(Lists.newArrayList(
				interactiveGRP,
				automaticGRP
				));
		when(form.getName()).thenReturn("form");
		return new RegistrationMVELContext(form, request, RequestSubmitStatus.submitted,
				TriggeringMode.manualAtLogin, false, "requestId");
	}
}
