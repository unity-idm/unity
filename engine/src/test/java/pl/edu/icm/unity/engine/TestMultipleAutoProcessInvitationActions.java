/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static com.google.common.collect.Lists.newArrayList;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.invitation.FormPrefill;
import pl.edu.icm.unity.base.registration.invitation.PrefilledEntryMode;
import pl.edu.icm.unity.base.registration.invitation.RegistrationInvitationParam;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessInvitationsActionFactory;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class TestMultipleAutoProcessInvitationActions extends DBIntegrationTestBase
{
	@Autowired
	private InvitationManagement invitationMan;
	@Autowired
	private InitializerCommon commonInitializer;
	
	@Before
	public void init() throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		groupsMan.addGroup(new Group("/A0"));
		groupsMan.addGroup(new Group("/A0/A1"));
		groupsMan.addGroup(new Group("/A0/A2"));
	}
	
	@Test
	public void shouldProcessAllAutoAcceptInvitationActions() throws EngineException
	{
		shouldProcessAllAutoAcceptInvitationActions(
				new TestInputData("form1", newArrayList("form1", "form2")),
				new TestInputData("form2", Collections.emptyList())
		);
	}
	
	@Test
	public void shouldProcessAllAutoAcceptInvitationActionsWhenNullFormName() throws EngineException
	{
		shouldProcessAllAutoAcceptInvitationActions(
				new TestInputData("form1", newArrayList((String) null)),
				new TestInputData("form2", Collections.emptyList())
		);
	}
	
	@Test
	public void shouldProcessAllAutoAcceptInvitationActionsWhenEmptyFormName() throws EngineException
	{
		shouldProcessAllAutoAcceptInvitationActions(
				new TestInputData("form1", newArrayList("")),
				new TestInputData("form2", Collections.emptyList())
		);
	}
	
	public void shouldProcessAllAutoAcceptInvitationActions(TestInputData input1, TestInputData input2)
			throws EngineException
	{
		// given
		createTestFormWithAutoAcceptInvitations(input1.formName, input1.autoProcessInvitationFormNames);
		createTestFormWithAutoAcceptInvitations(input2.formName, input2.autoProcessInvitationFormNames);
		
		String code = createAndGetInvitationCode(input1.formName, "cn1", "/A0/A1");
		createAndGetInvitationCode(input2.formName, "cn2", "/A0/A2");
		
		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId(input1.formName)
				.withRegistrationCode(code)
				.withAddedAttribute(
						VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "email1@email.io"))
				.withAddedAttribute(null)
				.withAddedGroupSelection(null)
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "invitedUser"))
				.build();
		
		// when
		registrationsMan.submitRegistrationRequest(request, new RegistrationContext(
				false, TriggeringMode.manualStandalone));
		
		// then
		AttributesAssertion.assertThat(attrsMan, UsernameIdentity.ID, "invitedUser")
				.hasAttribute(InitializerCommon.EMAIL_ATTR, "/").count(1)
				.hasAttribute(InitializerCommon.CN_ATTR, "/A0/A2").count(1).attr(0).hasValues("cn2")
				.hasAttribute(InitializerCommon.CN_ATTR, "/A0/A1").count(1).attr(0).hasValues("cn1");
	}
	
	private static class TestInputData
	{
		final String formName;
		final List<String> autoProcessInvitationFormNames;
		TestInputData(String formName, List<String> autoProcessInvitationFormNames)
		{
			this.formName = formName;
			this.autoProcessInvitationFormNames = autoProcessInvitationFormNames;
		}
	}
	
	private String createAndGetInvitationCode(String formName, String cnName, String groupName) throws EngineException
	{
		RegistrationInvitationParam invitation = RegistrationInvitationParam.builder()
				.withContactAddress("email1@email.io")
				.withExpiration(Instant.now().plusSeconds(100))
				.withForm(FormPrefill.builder() 
				.withForm(formName)
				.withAttribute(
						VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "email1@email.io"), 
						PrefilledEntryMode.DEFAULT)
				.withAttribute(
						StringAttribute.of(InitializerCommon.CN_ATTR, "", cnName),
						PrefilledEntryMode.HIDDEN)
				.withGroup(groupName, PrefilledEntryMode.HIDDEN).build())
				.build();
		return invitationMan.addInvitation(invitation);
	}

	private void createTestFormWithAutoAcceptInvitations(String formName, List<String> autoProcessInvitationForms)
			throws EngineException
	{
		TranslationAction autoAcceptRequest = new TranslationAction(
				AutoProcessActionFactory.NAME, AutomaticRequestAction.accept.toString());
		ArrayList<TranslationRule> rules = Lists.newArrayList(new TranslationRule("true", autoAcceptRequest));
		
		List<TranslationRule> autoProcessRules = autoProcessInvitationForms.stream()
				.map(autoProcessInvitationForm -> 
					new TranslationAction(AutoProcessInvitationsActionFactory.NAME, autoProcessInvitationForm))
				.map(action -> new TranslationRule("true", action))
				.collect(Collectors.toList());
		rules.addAll(autoProcessRules);
		
		TranslationProfile translationProfile = new TranslationProfile(formName, "description",
				ProfileType.REGISTRATION, rules);
		
		RegistrationForm form = new RegistrationFormBuilder()
				.withName(formName)
				.withDescription("desc")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPubliclyAvailable(true)
				.withTranslationProfile(translationProfile)
				.withAddedIdentityParam()
					.withIdentityType(UsernameIdentity.ID)
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endIdentityParam()
				.withAddedAttributeParam()
					.withAttributeType(InitializerCommon.EMAIL_ATTR).withGroup("/")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withOptional(true)
					.withShowGroups(true)
				.endAttributeParam()
				.withAddedAttributeParam()
					.withAttributeType(InitializerCommon.CN_ATTR).withGroup("DYN:/A0/*")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withOptional(true)
					.withShowGroups(true)
				.endAttributeParam()
				.withAddedGroupParam()
					.withGroupPath("/A0/*")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam()
				.build();
		registrationsMan.addForm(form);
	}
}
