/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;


import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.identity.IdentityParam;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.base.registration.invitation.InvitationParam;
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


@ExtendWith(SpringExtension.class)
public class TestAutoProcessInvitations extends DBIntegrationTestBase
{
	public static Object[][] notAutoAppliedModesProvider(){
		return new Object[][] {
				{
					PrefilledEntryMode.READ_ONLY,
				},
				{
					PrefilledEntryMode.DEFAULT
				}
			};
		}
	
	private static final String TEST_1_FORM = "form1";
	private static final String TEST_2_FORM = "form2";
	private static final RegistrationContext REG_CONTEXT_TRY_AUTO_ACCEPT = new RegistrationContext(
			false, TriggeringMode.manualStandalone);
	
	@Autowired
	private InvitationManagement invitationMan;
	@Autowired
	private InitializerCommon commonInitializer;
	
	@BeforeEach
	public void init() throws EngineException
	{
		commonInitializer.initializeCommonAttributeTypes();
		groupsMan.addGroup(new Group("/A0"));
		groupsMan.addGroup(new Group("/A0/A1"));
		groupsMan.addGroup(new Group("/A0/A2"));
	}
	
	@Test
	public void shouldAutoAcceptInvitationOfSameForm() throws EngineException
	{
		// given
		createTestFormWithAutoAcceptInvitations(TEST_1_FORM, TEST_1_FORM);
		
		// invitation for auto processing
		createInvitationToAutoAccept(TEST_1_FORM);
		
		// invitation for manual submit
		InvitationParam invitationToSubmit = RegistrationInvitationParam.builder()
				.withForm(TEST_1_FORM)
				.withExpiration(Instant.now().plusSeconds(100))
				.build();
		String code = invitationMan.addInvitation(invitationToSubmit);
		RegistrationRequest request = getRequest(TEST_1_FORM, code);
		
		// when
		registrationsMan.submitRegistrationRequest(request, REG_CONTEXT_TRY_AUTO_ACCEPT);
		
		// then
		AttributesAssertion.assertThat(attrsMan, UsernameIdentity.ID, "invitedUser")
			.hasAttribute(InitializerCommon.EMAIL_ATTR, "/").count(1)
			.hasAttribute(InitializerCommon.CN_ATTR, "/A0/A1").count(1)
				.attr(0).hasValues("commonName");
	}
	
	@ParameterizedTest
	@MethodSource("notAutoAppliedModesProvider")
	public void shouldNotTakePrefilledAttributeWithMode(PrefilledEntryMode mode) throws EngineException
	{
		// given
		createTestFormWithAutoAcceptInvitations(TEST_1_FORM, TEST_1_FORM);
		
		// invitation for auto processing
		InvitationParam invitation = RegistrationInvitationParam.builder()
				.withForm(TEST_1_FORM)
				.withContactAddress("email1@email.io")
				.withExpiration(Instant.now().plusSeconds(100))
				.withAttribute(
						VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "email1@email.io"), 
						PrefilledEntryMode.DEFAULT)
				.withAttribute(
						StringAttribute.of(InitializerCommon.CN_ATTR, "/A0/A1", "commonName"),
						mode)
				.withGroups(Collections.emptyList(), PrefilledEntryMode.DEFAULT)
				.withGroup("/A0/A1", mode)
				.build();
		invitationMan.addInvitation(invitation);
		
		// invitation for manual submit
		InvitationParam invitationToSubmit = RegistrationInvitationParam.builder()
				.withForm(TEST_1_FORM)
				.withExpiration(Instant.now().plusSeconds(100))
				.build();
		String code = invitationMan.addInvitation(invitationToSubmit);
		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId(TEST_1_FORM)
				.withRegistrationCode(code)
				.withAddedAttribute(
						VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "email1@email.io"))
				.withAddedAttribute(null)
				.withAddedGroupSelection().withGroup("/A0").endGroupSelection()
				.withAddedGroupSelection().endGroupSelection()
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "invitedUser"))
				.build();
		
		// when
		registrationsMan.submitRegistrationRequest(request, REG_CONTEXT_TRY_AUTO_ACCEPT);
		
		// then
		AttributesAssertion.assertThat(attrsMan, UsernameIdentity.ID, "invitedUser")
			.hasAttribute(InitializerCommon.EMAIL_ATTR, "/").count(1);
		GroupContents content = groupsMan.getContents("/A0/A1", GroupContents.MEMBERS);
		assertThat(content.getMembers().size()).isEqualTo(0);
	}
	
	@Test
	public void shouldDeleteAutoAcceptedInvitation() throws EngineException
	{
		// given
		createTestFormWithAutoAcceptInvitations(TEST_1_FORM, TEST_1_FORM);
		
		// invitation for auto processing
		String codeOfAutoAcceptedInvitation = createInvitationToAutoAccept(TEST_1_FORM);
		
		// invitation for manual submit
		InvitationParam invitationToSubmit = RegistrationInvitationParam.builder()
				.withForm(TEST_1_FORM)
				.withExpiration(Instant.now().plusSeconds(100))
				.build();
		String code = invitationMan.addInvitation(invitationToSubmit);
		RegistrationRequest request = getRequest(TEST_1_FORM, code);
		
		// expect
		Throwable error = catchThrowable(() -> invitationMan.getInvitation(codeOfAutoAcceptedInvitation));
		assertThat(error).isNull();
		
		// when
		registrationsMan.submitRegistrationRequest(request, REG_CONTEXT_TRY_AUTO_ACCEPT);
		
		// then
		error = catchThrowable(() -> invitationMan.getInvitation(codeOfAutoAcceptedInvitation));
		assertThat(error).isInstanceOf(IllegalArgumentException.class);
	}
	
	@Test
	public void shouldAutoAcceptInvitationOfDifferentForm() throws EngineException
	{
		// given
		createTestFormWithAutoAcceptInvitations(TEST_1_FORM, TEST_2_FORM);
		createTestFormWithAutoAcceptInvitations(TEST_2_FORM, TEST_2_FORM);
		
		createInvitationsAndVerify();
	}
	
	@Test
	public void shouldAutoAcceptAllInvitations() throws EngineException
	{
		// given
		createTestFormWithAutoAcceptInvitations(TEST_1_FORM, null);
		createTestFormWithAutoAcceptInvitations(TEST_2_FORM, TEST_2_FORM);
		
		createInvitationsAndVerify();
	}
	
	private void createInvitationsAndVerify() throws EngineException
	{
		
		// invitation for auto processing
		createInvitationToAutoAccept(TEST_2_FORM);
		
		// invitation for manual submit
		InvitationParam invitationToSubmit = RegistrationInvitationParam.builder()
				.withForm(TEST_1_FORM)
				.withExpiration(Instant.now().plusSeconds(100))
				.build();
		String code = invitationMan.addInvitation(invitationToSubmit);
		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId(TEST_1_FORM)
				.withRegistrationCode(code)
				.withAddedAttribute(
						VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "email1@email.io"))
				.withAddedAttribute(
						StringAttribute.of(InitializerCommon.CN_ATTR, "/A0/A1", "commonName1"))
				.withAddedGroupSelection().withGroup("/A0").endGroupSelection()
				.withAddedGroupSelection().endGroupSelection()
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "invitedUser"))
				.build();
		
		// when
		registrationsMan.submitRegistrationRequest(request, REG_CONTEXT_TRY_AUTO_ACCEPT);
		
		// then
		AttributesAssertion.assertThat(attrsMan, UsernameIdentity.ID, "invitedUser")
				.hasAttribute(InitializerCommon.EMAIL_ATTR, "/").count(1)
				.hasAttribute(InitializerCommon.CN_ATTR, "/A0/A1").count(1)
						.attr(0).hasValues("commonName");
		
	}

	private String createInvitationToAutoAccept(String form) throws EngineException
	{
		InvitationParam invitation = RegistrationInvitationParam.builder()
			.withForm(form)
			.withContactAddress("email1@email.io")
			.withExpiration(Instant.now().plusSeconds(100))
			.withAttribute(
					VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "email1@email.io"), 
					PrefilledEntryMode.DEFAULT)
			.withAttribute(
					StringAttribute.of(InitializerCommon.CN_ATTR, "/A0/A1", "commonName"),
					PrefilledEntryMode.HIDDEN)
			.withGroups(Collections.emptyList(), PrefilledEntryMode.DEFAULT)
			.withGroup("/A0/A1", PrefilledEntryMode.HIDDEN)
			.build();
		return invitationMan.addInvitation(invitation);
	}

	private RegistrationForm createTestFormWithAutoAcceptInvitations(String formName, 
			String autoProcessInvitationForm) throws EngineException
	{
		RegistrationForm form = getForm(formName, autoProcessInvitationForm);
		registrationsMan.addForm(form);
		return form;
	}
	
	private RegistrationRequest getRequest(String formName, String code)
	{
		return new RegistrationRequestBuilder()
				.withFormId(formName)
				.withRegistrationCode(code)
				.withAddedAttribute(
						VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "email1@email.io"))
				.withAddedAttribute(null)
				.withAddedGroupSelection().withGroup("/A0").endGroupSelection()
				.withAddedGroupSelection().endGroupSelection()
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "invitedUser"))
				.build();
	}
	
	private RegistrationForm getForm(String formName, String autoProcessInvitationForm)
	{
		TranslationAction autoAcceptRequest = new TranslationAction(
				AutoProcessActionFactory.NAME, AutomaticRequestAction.accept.toString());
		TranslationAction autoProcessInvitations = new TranslationAction(
				AutoProcessInvitationsActionFactory.NAME, autoProcessInvitationForm);
		TranslationProfile translationProfile = 
				new TranslationProfile(formName, "description", ProfileType.REGISTRATION, 
						Lists.newArrayList(
								new TranslationRule("true", autoAcceptRequest),
								new TranslationRule("true", autoProcessInvitations)));
		
		return new RegistrationFormBuilder()
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
					.withAttributeType(InitializerCommon.CN_ATTR).withGroup("/A0/A1")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
					.withOptional(true)
					.withShowGroups(true)
				.endAttributeParam()
				.withAddedGroupParam()
					.withGroupPath("/A0")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam()
				.withAddedGroupParam()
					.withGroupPath("/A0/A1")
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endGroupParam()
				.build();
	}
	
	
	@Test
	public void shouldAutoAcceptAttributesFromContextualGroup() throws EngineException
	{
		// given
		createTestFormWithAutoAcceptInvitationsAndContextGroup(TEST_1_FORM);
		
		InvitationParam invitation = RegistrationInvitationParam.builder()
				.withForm(TEST_1_FORM)
				.withContactAddress("email1@email.io")
				.withExpiration(Instant.now().plusSeconds(100))
				.withAttribute(
						VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "email1@email.io"), 
						PrefilledEntryMode.DEFAULT)
				.withAttribute(
						StringAttribute.of(InitializerCommon.CN_ATTR, "", "cn1"),
						PrefilledEntryMode.HIDDEN)
				.withGroup("/A0/A1", PrefilledEntryMode.HIDDEN)
				.build();
		invitationMan.addInvitation(invitation);
		
		InvitationParam invitationToSubmit = RegistrationInvitationParam.builder()
				.withForm(TEST_1_FORM)
				.withExpiration(Instant.now().plusSeconds(100))
				.withContactAddress("email1@email.io")
				.withExpiration(Instant.now().plusSeconds(100))
				.withAttribute(
						VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "email1@email.io"), 
						PrefilledEntryMode.DEFAULT)
				.withAttribute(
						StringAttribute.of(InitializerCommon.CN_ATTR, "", "cn2"),
						PrefilledEntryMode.HIDDEN)
				.withGroup("/A0/A2", PrefilledEntryMode.HIDDEN)
				.build();
		String code = invitationMan.addInvitation(invitationToSubmit);
		RegistrationRequest request = new RegistrationRequestBuilder()
				.withFormId(TEST_1_FORM)
				.withRegistrationCode(code)
				.withAddedAttribute(
						VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "email1@email.io"))
				.withAddedAttribute(null)
				.withAddedGroupSelection(null)
				.withAddedIdentity(new IdentityParam(UsernameIdentity.ID, "invitedUser"))
				.build();
		
		// when
		registrationsMan.submitRegistrationRequest(request, REG_CONTEXT_TRY_AUTO_ACCEPT);
		
		// then
		AttributesAssertion.assertThat(attrsMan, UsernameIdentity.ID, "invitedUser")
				.hasAttribute(InitializerCommon.EMAIL_ATTR, "/").count(1)
				.hasAttribute(InitializerCommon.CN_ATTR, "/A0/A2").count(1).attr(0).hasValues("cn2")
				.hasAttribute(InitializerCommon.CN_ATTR, "/A0/A1").count(1).attr(0).hasValues("cn1");
		
	}

	private void createTestFormWithAutoAcceptInvitationsAndContextGroup(String formName) throws EngineException
	{
		TranslationAction autoAcceptRequest = new TranslationAction(
				AutoProcessActionFactory.NAME, AutomaticRequestAction.accept.toString());
		TranslationAction autoProcessInvitations = new TranslationAction(
				AutoProcessInvitationsActionFactory.NAME, formName);
		TranslationProfile translationProfile = 
				new TranslationProfile(formName, "description", ProfileType.REGISTRATION, 
						Lists.newArrayList(
								new TranslationRule("true", autoAcceptRequest),
								new TranslationRule("true", autoProcessInvitations)));
		
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
