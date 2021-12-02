/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.hamcrest.text.IsEmptyString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.base.msgtemplates.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.InvitationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.NewEnquiryTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.base.msgtemplates.reg.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.engine.utils.GroupDelegationConfigGeneratorImpl;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

@RunWith(MockitoJUnitRunner.class)
public class TestGroupDelegationConfigGenerator extends TestProjectBase
{
	private GroupDelegationConfigGeneratorImpl generator;

	@Mock
	private MessageTemplateDB mockMsgTemplateDB;
	@Mock
	private EnquiryFormDB mockEnqFormDB;

	@Mock
	private GroupDAO mockGroupDB;

	@Mock
	private RegistrationFormDB mockRegistrationFormDB;

	@Before
	public void init()
	{
		generator = new GroupDelegationConfigGeneratorImpl(mockMsg, mockRegistrationFormDB, mockMsgTemplateDB,
				mockEnqFormDB, mockAttrHelper, mockGroupDB);
	}

	@Test
	public void shouldGenerateCorrectProjectRegistrationForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.registrationNameSuffix"))).thenReturn("regSuffix");
		when(mockRegistrationFormDB.getAll()).thenReturn(List.of());
		when(mockAttrHelper.getAttributeTypeWithSingeltonMetadata(eq(EntityNameMetadataProvider.NAME)))
				.thenReturn(new AttributeType("name", null));
		addGroup("/A", "a", true, true);
		addTemplates();
		RegistrationForm form = generator.generateProjectRegistrationForm("/A", "https://logo.url",
				Arrays.asList("at1"));
		assertThat(form.getName(), is("aregSuffix"));
		assertThat(form.getAttributeParams().get(0).getAttributeType(), is("name"));
		assertThat(form.getAttributeParams().get(1).getAttributeType(), is("at1"));
		assertThat(form.getGroupParams().get(0).getGroupPath(), is("/A/?*/**"));
		assertThat(form.getIdentityParams().get(0).getIdentityType(), is(EmailIdentity.ID));
		assertAutomationProfile(form.getTranslationProfile(), "/A");
		assertNotificationTemplates(form.getNotificationsConfiguration());
	}

	@Test
	public void shouldGenerateCorrectProjectJoinEnquiryForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.joinEnquiryNameSuffix"))).thenReturn("enSuffix");
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		addGroup("/A", "a", true, true);
		addTemplates();
		EnquiryForm form = generator.generateProjectJoinEnquiryForm("/A", "https://logo.url");
		assertThat(form.getName(), is("aenSuffix"));
		assertThat(form.getTargetCondition(), is("!(groups contains '/A')"));
		assertThat(form.getGroupParams().get(0).getGroupPath(), is("/A/?*/**"));
		assertAutomationProfile(form.getTranslationProfile(), "/A");
		assertNotificationTemplates(form.getNotificationsConfiguration());
	}

	@Test
	public void shouldGenerateCorrectProjectUpdateEnquiryForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.updateEnquiryNameSuffix"))).thenReturn("enSuffix");
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		addGroup("/A", "a", true, true);
		addTemplates();
		EnquiryForm form = generator.generateProjectUpdateEnquiryForm("/A", "https://logo.url");
		assertThat(form.getName(), is("aenSuffix"));
		assertThat(form.getGroupParams().get(0).getGroupPath(), is("/A/?*/**"));
		assertThat(form.getTargetGroups()[0], is("/A"));
	}

	@Test
	public void shouldGenerateCorrectSubprojectRegistrationForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.registrationNameSuffix"))).thenReturn("regSuffix");
		when(mockRegistrationFormDB.getAll()).thenReturn(List.of());
		when(mockAttrHelper.getAttributeTypeWithSingeltonMetadata(eq(EntityNameMetadataProvider.NAME)))
				.thenReturn(new AttributeType("name", null));
		addGroup("/A", "a", true, true);
		addTemplates();
		RegistrationForm form = generator.generateProjectRegistrationForm("/A", "https://logo.url",
				Arrays.asList("at1"));
		when(mockRegistrationFormDB.get(eq("aregSuffix"))).thenReturn(form);
		addGroup("/A/B", "ab", true, true);
		addTemplates();
		RegistrationForm sform = generator.generateSubprojectRegistrationForm("aregSuffix", "/A", "/A/B",
				"https://logo.url");
		assertThat(sform.getName(), is("abregSuffix"));
		assertThat(sform.getAttributeParams().get(0).getAttributeType(), is("name"));
		assertThat(sform.getAttributeParams().get(1).getAttributeType(), is("at1"));
		assertThat(sform.getGroupParams().get(0).getGroupPath(), is("/A/B/?*/**"));
		assertThat(sform.getIdentityParams().get(0).getIdentityType(), is(EmailIdentity.ID));
		assertAutomationProfile(sform.getTranslationProfile(), "/A");
		assertNotificationTemplates(sform.getNotificationsConfiguration());
	}

	@Test
	public void shouldGenerateCorrectSubprojectJoinEnquiryForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.joinEnquiryNameSuffix"))).thenReturn("enSuffix");
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		addGroup("/A", "a", true, true);
		addTemplates();
		EnquiryForm form = generator.generateProjectJoinEnquiryForm("/A", "https://logo.url");
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);
		addGroup("/A/B", "ab", true, true);
		addTemplates();
		EnquiryForm sform = generator.generateSubprojectJoinEnquiryForm("aenSuffix", "/A", "/A/B",
				"https://logo.url");
		assertThat(sform.getName(), is("abenSuffix"));
		assertThat(sform.getTargetCondition(), is("!(groups contains '/A/B')"));
		assertThat(sform.getGroupParams().get(0).getGroupPath(), is("/A/B/?*/**"));
		assertAutomationProfile(sform.getTranslationProfile(), "/A/B");
		assertNotificationTemplates(sform.getNotificationsConfiguration());
	}

	@Test
	public void shouldGenerateCorrectSubprojectUpdateEnquiryForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.updateEnquiryNameSuffix"))).thenReturn("enSuffix");
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		addGroup("/A", "a", true, true);
		addTemplates();
		EnquiryForm form = generator.generateProjectUpdateEnquiryForm("/A", "https://logo.url");
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);
		addGroup("/A/B", "ab", true, true);
		addTemplates();
		EnquiryForm sform = generator.generateSubprojectUpdateEnquiryForm("aenSuffix", "/A", "/A/B",
				"https://logo.url");
		assertThat(sform.getName(), is("abenSuffix"));
		assertThat(sform.getGroupParams().get(0).getGroupPath(), is("/A/B/?*/**"));
		assertThat(sform.getTargetGroups()[0], is("/A/B"));
	}

	@Test
	public void shouldValidateRegistrationForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.registrationNameSuffix"))).thenReturn("regSuffix");
		when(mockRegistrationFormDB.getAll()).thenReturn(List.of());
		when(mockAttrHelper.getAttributeTypeWithSingeltonMetadata(eq(EntityNameMetadataProvider.NAME)))
				.thenReturn(new AttributeType("name", null));
		addGroup("/A", "a", true, true);
		addTemplates();
		RegistrationForm form = generator.generateProjectRegistrationForm("/A", "https://logo.url",
				Arrays.asList("at1"));
		when(mockRegistrationFormDB.get(eq("aregSuffix"))).thenReturn(form);

		List<String> errors = generator.validateRegistrationForm("/A", "aregSuffix");
		assertThat(errors.size(), is(0));
	}

	@Test
	public void shouldValidateJoinEnquiryForm() throws EngineException
	{
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		addGroup("/A", "a", true, true);
		addTemplates();
		EnquiryForm form = generator.generateProjectJoinEnquiryForm("/A", "https://logo.url");
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);

		List<String> errors = generator.validateJoinEnquiryForm("/A", "aenSuffix");
		assertThat(errors.size(), is(0));
	}

	@Test
	public void shouldValidateUpdateEnquiryForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.updateEnquiryNameSuffix"))).thenReturn("enSuffix");
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		addGroup("/A", "a", true, true);
		addTemplates();
		EnquiryForm form = generator.generateProjectUpdateEnquiryForm("/A", "https://logo.url");
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);

		List<String> errors = generator.validateUpdateEnquiryForm("/A", "aenSuffix");
		assertThat(errors.size(), is(0));
	}

	@Test
	public void shouldThrowValidationErrorsWhenRegistrationFormIsIncorrect() throws EngineException
	{
		RegistrationForm form = new RegistrationFormBuilder().withName("aregSuffix")
				.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.build();
		when(mockRegistrationFormDB.get(eq("aregSuffix"))).thenReturn(form);
		when(mockMsg.getMessage(anyString())).thenAnswer(i -> i.getArguments()[0]);
		List<String> errors = generator.validateRegistrationForm("/A", "aregSuffix");
		assertThat(errors.get(0), is("FormGenerator.noEmailIdentity"));
		assertThat(errors.get(1), is("FormGenerator.noAutoAccept"));
		assertThat(errors.get(2), is("FormGenerator.noAutoGroupAdd"));
		assertThat(errors.get(3), is("FormGenerator.noInvitationTemplate"));
		assertThat(errors.get(4), is("FormGenerator.noAcceptTemplate"));
		assertThat(errors.get(5), is("FormGenerator.noRejectTemplate"));
		assertThat(errors.get(6), is("FormGenerator.noUpdateTemplate"));
	}

	@Test
	public void shouldThrowValidationErrorsWhenJoinEnquiryFormIsIncorrect() throws EngineException
	{
		EnquiryForm form = new EnquiryFormBuilder().withName("aenSuffix").withTargetGroups(new String[] { "/" })
				.withType(EnquiryForm.EnquiryType.STICKY).withTargetCondition("!(groups contains '/B')")
				.build();
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);
		when(mockMsg.getMessage(anyString())).thenAnswer(i -> i.getArguments()[0]);
		List<String> errors = generator.validateJoinEnquiryForm("/A", "aenSuffix");
		assertThat(errors.get(0), is("FormGenerator.noAutoAccept"));
		assertThat(errors.get(1), is("FormGenerator.noAutoGroupAdd"));
		assertThat(errors.get(2), is("FormGenerator.noInvitationTemplate"));
		assertThat(errors.get(3), is("FormGenerator.noAcceptTemplate"));
		assertThat(errors.get(4), is("FormGenerator.noRejectTemplate"));
		assertThat(errors.get(5), is("FormGenerator.noUpdateTemplate"));
	}

	@Test
	public void shouldThrowValidationErrorsWhenUpdateEnquiryFormIsIncorrect() throws EngineException
	{
		EnquiryForm form = new EnquiryFormBuilder().withName("aenSuffix").withTargetGroups(new String[] { "/" })
				.withType(EnquiryForm.EnquiryType.STICKY).withTargetCondition("!(groups contains '/B')")
				.build();
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);
		when(mockMsg.getMessage(anyString())).thenAnswer(i -> i.getArguments()[0]);
		List<String> errors = generator.validateUpdateEnquiryForm("/A", "aenSuffix");
		assertThat(errors.get(0), is("FormGenerator.targetGroupWithoutProject"));
	}

	private void addGroup(String path, String name, boolean groupWithEnabledDelegation, boolean enableSubproject)
	{
		Group group = new Group(path);
		group.setDisplayedName(new I18nString(name));
		group.setDelegationConfiguration(new GroupDelegationConfiguration(groupWithEnabledDelegation,
				enableSubproject, null, null, null, null, List.of()));
		when(mockGroupDB.get(eq(path))).thenReturn(group);
	}

	private void addTemplates()
	{
		when(mockMsgTemplateDB.getAll()).thenReturn(List.of(getTemplate(InvitationTemplateDef.NAME),
				getTemplate(RejectRegistrationTemplateDef.NAME),
				getTemplate(AcceptRegistrationTemplateDef.NAME),
				getTemplate(UpdateRegistrationTemplateDef.NAME),
				getTemplate(NewEnquiryTemplateDef.NAME)));
	}

	private MessageTemplate getTemplate(String type)
	{
		return new MessageTemplate(type, "", new I18nMessage(new I18nString("title"), new I18nString("body")),
				type, MessageType.PLAIN, "channel");
	}

	private void assertAutomationProfile(TranslationProfile profile, String group)
	{
		List<? extends TranslationRule> rules = profile.getRules();
		assertThat(rules.stream()
				.filter(r -> r.getAction().getName().equals(AutoProcessActionFactory.NAME)
						&& !(r.getAction().getParameters().length == 0)
						&& r.getAction().getParameters()[0]
								.equals(AutomaticRequestAction.accept.toString()))
				.findFirst().isPresent(), is(true));

		assertThat(rules.stream()
				.filter(r -> r.getAction().getName().equals(AddToGroupActionFactory.NAME)
						&& !(r.getAction().getParameters().length == 0)
						&& r.getAction().getParameters()[0].contains(group))
				.findFirst().isPresent(), is(true));
	}

	private void assertNotificationTemplates(BaseFormNotifications notConfig)
	{
		assertThat(notConfig.getInvitationTemplate(), not(IsEmptyString.emptyOrNullString()));
		assertThat(notConfig.getAcceptedTemplate(), not(IsEmptyString.emptyOrNullString()));
		assertThat(notConfig.getRejectedTemplate(), not(IsEmptyString.emptyOrNullString()));
		assertThat(notConfig.getUpdatedTemplate(), not(IsEmptyString.emptyOrNullString()));
	}
}
