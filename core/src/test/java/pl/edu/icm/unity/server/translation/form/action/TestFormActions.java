/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.translation.form.action;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.MockAttributeSyntax;
import pl.edu.icm.unity.server.api.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.server.api.internal.AttributesInternalProcessing;
import pl.edu.icm.unity.server.translation.form.GroupParam;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile;
import pl.edu.icm.unity.server.translation.form.RegistrationTranslationProfile.RequestSubmitStatus;
import pl.edu.icm.unity.server.translation.form.TranslatedRegistrationRequest;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.AttributeVisibility;
import pl.edu.icm.unity.types.basic.IdentityParam;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;

public class TestFormActions
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
	public void testfilterAttribute() throws EngineException
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
	public void testfilterGroup() throws EngineException
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
	public void testfilterIdentity() throws EngineException
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
		
		//TODO test rest, move to other class
	}
	
	@SuppressWarnings("unchecked")
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
		
		
		RegistrationForm form = mock(RegistrationForm.class);
		AttributeRegistrationParam interactiveARP = new AttributeRegistrationParam();
		interactiveARP.setRetrievalSettings(ParameterRetrievalSettings.interactive);
		AttributeRegistrationParam automaticARP = new AttributeRegistrationParam();
		automaticARP.setRetrievalSettings(ParameterRetrievalSettings.automaticHidden);
		when(form.getAttributeParams()).thenReturn(Lists.newArrayList(
				interactiveARP,
				automaticARP
				));
		
		
		return RegistrationTranslationProfile.createMvelContext(form, request, RequestSubmitStatus.submitted,
				TriggeringMode.manualAtLogin, false, "requestId");
	}
}
