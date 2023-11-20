/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.i18n.I18nMessage;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.msg_template.MessageTemplate;
import pl.edu.icm.unity.base.msg_template.MessageType;
import pl.edu.icm.unity.base.msg_template.reg.AcceptRegistrationTemplateDef;
import pl.edu.icm.unity.base.msg_template.reg.InvitationTemplateDef;
import pl.edu.icm.unity.base.msg_template.reg.NewEnquiryTemplateDef;
import pl.edu.icm.unity.base.msg_template.reg.RejectRegistrationTemplateDef;
import pl.edu.icm.unity.base.msg_template.reg.UpdateRegistrationTemplateDef;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementConfiguration;
import pl.edu.icm.unity.base.policy_agreement.PolicyAgreementPresentationType;
import pl.edu.icm.unity.base.registration.BaseFormNotifications;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AutoProcessActionFactory;
import pl.edu.icm.unity.engine.utils.GroupDelegationConfigGeneratorImpl;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.PolicyDocumentDAO;
import pl.edu.icm.unity.store.api.generic.EnquiryFormDB;
import pl.edu.icm.unity.store.api.generic.MessageTemplateDB;
import pl.edu.icm.unity.store.api.generic.RegistrationFormDB;
import pl.edu.icm.unity.store.types.StoredPolicyDocument;

@ExtendWith(MockitoExtension.class)
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

	@Mock
	private PolicyDocumentDAO mockPolicyDocumentDB;
	
	@BeforeEach
	public void init()
	{
		generator = new GroupDelegationConfigGeneratorImpl(mockMsg, mockRegistrationFormDB, mockMsgTemplateDB,
				mockEnqFormDB, mockAttrHelper, mockGroupDB, mockPolicyDocumentDB);
	}

	@Test
	public void shouldGenerateCorrectProjectRegistrationForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.registrationNameSuffix"))).thenReturn("regSuffix");
		when(mockRegistrationFormDB.getAll()).thenReturn(List.of());
		when(mockAttrHelper.getAttributeTypeWithSingeltonMetadata(eq(EntityNameMetadataProvider.NAME)))
				.thenReturn(new AttributeType("name", null));
		when(mockPolicyDocumentDB.getByKey(1L)).thenReturn(new StoredPolicyDocument(1L, "Policy"));
		addGroup("/A", "a", true, true);
		addTemplates();
		RegistrationForm form = generator.generateProjectRegistrationForm("/A", "https://logo.url",
				Arrays.asList("at1"), List.of(1L));
		assertThat(form.getName()).isEqualTo("aregSuffix");
		assertThat(form.getAttributeParams().get(0).getAttributeType()).isEqualTo("name");
		assertThat(form.getAttributeParams().get(1).getAttributeType()).isEqualTo("at1");
		assertThat(form.getGroupParams().get(0).getGroupPath()).isEqualTo("/A/?*/**");
		assertThat(form.getIdentityParams().get(0).getIdentityType()).isEqualTo(EmailIdentity.ID);
		assertThat(form.getPolicyAgreements().get(0).documentsIdsToAccept.get(0)).isEqualTo(1L);
		assertAutomationProfile(form.getTranslationProfile(), "/A");
		assertNotificationTemplates(form.getNotificationsConfiguration());
	}

	@Test
	public void shouldGenerateCorrectProjectJoinEnquiryForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.joinEnquiryNameSuffix"))).thenReturn("enSuffix");
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		when(mockPolicyDocumentDB.getByKey(1L)).thenReturn(new StoredPolicyDocument(1L, "Policy"));
		addGroup("/A", "a", true, true);
		addTemplates();
		EnquiryForm form = generator.generateProjectJoinEnquiryForm("/A", "https://logo.url", List.of(1L));
		assertThat(form.getName()).isEqualTo("aenSuffix");
		assertThat(form.getTargetCondition()).isEqualTo("!(groups contains '/A')");
		assertThat(form.getGroupParams().get(0).getGroupPath()).isEqualTo("/A/?*/**");
		assertThat(form.getPolicyAgreements().get(0).documentsIdsToAccept.get(0)).isEqualTo(1L);
		assertAutomationProfile(form.getTranslationProfile(), "/A");
		assertNotificationTemplates(form.getNotificationsConfiguration());
	}

	@Test
	public void shouldGenerateCorrectProjectUpdateEnquiryForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.updateEnquiryNameSuffix"))).thenReturn("enSuffix");
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		addGroup("/A", "a", true, true);
		EnquiryForm form = generator.generateProjectUpdateEnquiryForm("/A", "https://logo.url");
		assertThat(form.getName()).isEqualTo("aenSuffix");
		assertThat(form.getGroupParams().get(0).getGroupPath()).isEqualTo("/A/?*/**");
		assertThat(form.getTargetGroups()[0]).isEqualTo("/A");
	}

	@Test
	public void shouldGenerateCorrectSubprojectRegistrationForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.registrationNameSuffix"))).thenReturn("regSuffix");
		when(mockRegistrationFormDB.getAll()).thenReturn(List.of());
		when(mockAttrHelper.getAttributeTypeWithSingeltonMetadata(eq(EntityNameMetadataProvider.NAME)))
				.thenReturn(new AttributeType("name", null));
		when(mockPolicyDocumentDB.getByKey(1L)).thenReturn(new StoredPolicyDocument(1L, "Policy"));
		addGroup("/A", "a", true, true);
		addTemplates();
		RegistrationForm form = generator.generateProjectRegistrationForm("/A", "https://logo.url",
				Arrays.asList("at1"), List.of(1L));
		when(mockRegistrationFormDB.get(eq("aregSuffix"))).thenReturn(form);
		addGroup("/A/B", "ab", true, true);
		RegistrationForm sform = generator.generateSubprojectRegistrationForm("aregSuffix", "/A", "/A/B",
				"https://logo.url");
		assertThat(sform.getName()).isEqualTo("abregSuffix");
		assertThat(sform.getAttributeParams().get(0).getAttributeType()).isEqualTo("name");
		assertThat(sform.getAttributeParams().get(1).getAttributeType()).isEqualTo("at1");
		assertThat(sform.getGroupParams().get(0).getGroupPath()).isEqualTo("/A/B/?*/**");
		assertThat(sform.getIdentityParams().get(0).getIdentityType()).isEqualTo(EmailIdentity.ID);
		assertAutomationProfile(sform.getTranslationProfile(), "/A");
		assertNotificationTemplates(sform.getNotificationsConfiguration());
		assertPolicies(sform.getPolicyAgreements(), List.of(1L, 2L));
	}

	@Test
	public void shouldGenerateCorrectSubprojectJoinEnquiryForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.joinEnquiryNameSuffix"))).thenReturn("enSuffix");
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		when(mockPolicyDocumentDB.getByKey(1L)).thenReturn(new StoredPolicyDocument(1L, "Policy"));
		addGroup("/A", "a", true, true);
		addTemplates();
		EnquiryForm form = generator.generateProjectJoinEnquiryForm("/A", "https://logo.url", List.of(1L));
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);
		addGroup("/A/B", "ab", true, true);
		EnquiryForm sform = generator.generateSubprojectJoinEnquiryForm("aenSuffix", "/A", "/A/B",
				"https://logo.url");
		assertThat(sform.getName()).isEqualTo("abenSuffix");
		assertThat(sform.getTargetCondition()).isEqualTo("!(groups contains '/A/B')");
		assertThat(sform.getGroupParams().get(0).getGroupPath()).isEqualTo("/A/B/?*/**");
		assertAutomationProfile(sform.getTranslationProfile(), "/A/B");
		assertNotificationTemplates(sform.getNotificationsConfiguration());
		assertPolicies(sform.getPolicyAgreements(), List.of(1L, 2L));
	}

	@Test
	public void shouldGenerateCorrectSubprojectUpdateEnquiryForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.updateEnquiryNameSuffix"))).thenReturn("enSuffix");
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		addGroup("/A", "a", true, true);
		EnquiryForm form = generator.generateProjectUpdateEnquiryForm("/A", "https://logo.url");
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);
		addGroup("/A/B", "ab", true, true);
		EnquiryForm sform = generator.generateSubprojectUpdateEnquiryForm("aenSuffix", "/A", "/A/B",
				"https://logo.url");
		assertThat(sform.getName()).isEqualTo("abenSuffix");
		assertThat(sform.getGroupParams().get(0).getGroupPath()).isEqualTo("/A/B/?*/**");
		assertThat(sform.getTargetGroups()[0]).isEqualTo("/A/B");
	}

	@Test
	public void shouldValidateRegistrationForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.registrationNameSuffix"))).thenReturn("regSuffix");
		when(mockRegistrationFormDB.getAll()).thenReturn(List.of());
		when(mockAttrHelper.getAttributeTypeWithSingeltonMetadata(eq(EntityNameMetadataProvider.NAME)))
				.thenReturn(new AttributeType("name", null));
		when(mockPolicyDocumentDB.getByKey(1L)).thenReturn(new StoredPolicyDocument(1L, "Policy"));
		when(mockPolicyDocumentDB.getAll()).thenReturn(List.of(new StoredPolicyDocument(1L, "policy")));
		addGroup("/A", "a", true, true);
		addTemplates();
		RegistrationForm form = generator.generateProjectRegistrationForm("/A", "https://logo.url",
				Arrays.asList("at1"), List.of(1L));
		when(mockRegistrationFormDB.get(eq("aregSuffix"))).thenReturn(form);

		List<String> errors = generator.validateRegistrationForm("/A", "aregSuffix", Set.of(1L));
		assertThat(errors.size()).isEqualTo(0);
	}

	@Test
	public void shouldValidateJoinEnquiryForm() throws EngineException
	{
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		when(mockPolicyDocumentDB.getByKey(1L)).thenReturn(new StoredPolicyDocument(1L, "Policy"));
		when(mockPolicyDocumentDB.getAll()).thenReturn(List.of(new StoredPolicyDocument(1L, "policy")));

		addGroup("/A", "a", true, true);
		addTemplates();
		EnquiryForm form = generator.generateProjectJoinEnquiryForm("/A", "https://logo.url", List.of(1L));
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);

		List<String> errors = generator.validateJoinEnquiryForm("/A", "aenSuffix", Set.of(1L));
		assertThat(errors.size()).isEqualTo(0);
	}

	@Test
	public void shouldValidateUpdateEnquiryForm() throws EngineException
	{
		when(mockMsg.getMessage(eq("FormGenerator.updateEnquiryNameSuffix"))).thenReturn("enSuffix");
		when(mockEnqFormDB.getAll()).thenReturn(List.of());
		addGroup("/A", "a", true, true);
		EnquiryForm form = generator.generateProjectUpdateEnquiryForm("/A", "https://logo.url");
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);

		List<String> errors = generator.validateUpdateEnquiryForm("/A", "aenSuffix");
		assertThat(errors.size()).isEqualTo(0);
	}

	@Test
	public void shouldThrowValidationErrorsWhenRegistrationFormIsIncorrect() throws EngineException
	{
		RegistrationForm form = new RegistrationFormBuilder().withName("aregSuffix")
				.withDefaultCredentialRequirement(EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withPolicyAgreements(List.of(new PolicyAgreementConfiguration(List.of(1L),
						PolicyAgreementPresentationType.CHECKBOX_NOTSELECTED, new I18nString())))
				.build();
		when(mockRegistrationFormDB.get(eq("aregSuffix"))).thenReturn(form);
		when(mockPolicyDocumentDB.getAll()).thenReturn(List.of(new StoredPolicyDocument(1L, "policy")));
		when(mockMsg.getMessage(anyString())).thenAnswer(i -> i.getArguments()[0]);
		when(mockMsg.getMessage(anyString(), anyString())).thenAnswer(i -> i.getArguments()[0]);
		List<String> errors = generator.validateRegistrationForm("/A", "aregSuffix", Set.of(2L));
		assertThat(errors.get(0)).isEqualTo("FormGenerator.noEmailIdentity");
		assertThat(errors.get(1)).isEqualTo("FormGenerator.noAutoAccept");
		assertThat(errors.get(2)).isEqualTo("FormGenerator.noAutoGroupAdd");
		assertThat(errors.get(3)).isEqualTo("FormGenerator.noInvitationTemplate");
		assertThat(errors.get(4)).isEqualTo("FormGenerator.noAcceptTemplate");
		assertThat(errors.get(5)).isEqualTo("FormGenerator.noRejectTemplate");
		assertThat(errors.get(6)).isEqualTo("FormGenerator.noUpdateTemplate");
		assertThat(errors.get(7)).isEqualTo("FormGenerator.missingFormPolicies");
		assertThat(errors.get(8)).isEqualTo("FormGenerator.additionalFormPolicies");
	}

	@Test
	public void shouldThrowValidationErrorsWhenJoinEnquiryFormIsIncorrect() throws EngineException
	{
		EnquiryForm form = new EnquiryFormBuilder().withName("aenSuffix").withTargetGroups(new String[] { "/" })
				.withType(EnquiryForm.EnquiryType.STICKY).withTargetCondition("!(groups contains '/B')")
				.withPolicyAgreements(List.of(new PolicyAgreementConfiguration(List.of(1L),
						PolicyAgreementPresentationType.CHECKBOX_NOTSELECTED, new I18nString())))
				.build();
		when(mockEnqFormDB.get(eq("aenSuffix"))).thenReturn(form);
		when(mockMsg.getMessage(anyString())).thenAnswer(i -> i.getArguments()[0]);
		when(mockMsg.getMessage(anyString(), anyString())).thenAnswer(i -> i.getArguments()[0]);
		when(mockPolicyDocumentDB.getAll()).thenReturn(List.of(new StoredPolicyDocument(1L, "policy")));

		List<String> errors = generator.validateJoinEnquiryForm("/A", "aenSuffix", Set.of(2L));
		assertThat(errors.get(0)).isEqualTo("FormGenerator.noAutoAccept");
		assertThat(errors.get(1)).isEqualTo("FormGenerator.noAutoGroupAdd");
		assertThat(errors.get(2)).isEqualTo("FormGenerator.noInvitationTemplate");
		assertThat(errors.get(3)).isEqualTo("FormGenerator.noAcceptTemplate");
		assertThat(errors.get(4)).isEqualTo("FormGenerator.noRejectTemplate");
		assertThat(errors.get(5)).isEqualTo("FormGenerator.noUpdateTemplate");
		assertThat(errors.get(6)).isEqualTo("FormGenerator.missingFormPolicies");
		assertThat(errors.get(7)).isEqualTo("FormGenerator.additionalFormPolicies");
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
		assertThat(errors.get(0)).isEqualTo("FormGenerator.targetGroupWithoutProject");
	}

	private void addGroup(String path, String name, boolean groupWithEnabledDelegation, boolean enableSubproject)
	{
		Group group = new Group(path);
		group.setDisplayedName(new I18nString(name));
		group.setDelegationConfiguration(new GroupDelegationConfiguration(groupWithEnabledDelegation,
				enableSubproject, null, null, null, null, List.of(), List.of()));
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
				.findFirst().isPresent()).isEqualTo(true);

		assertThat(rules.stream()
				.filter(r -> r.getAction().getName().equals(AddToGroupActionFactory.NAME)
						&& !(r.getAction().getParameters().length == 0)
						&& r.getAction().getParameters()[0].contains(group))
				.findFirst().isPresent()).isEqualTo(true);
	}

	private void assertNotificationTemplates(BaseFormNotifications notConfig)
	{
		assertThat(notConfig.getInvitationTemplate()).isNotEmpty();
		assertThat(notConfig.getAcceptedTemplate()).isNotEmpty();
		assertThat(notConfig.getRejectedTemplate()).isNotEmpty();
		assertThat(notConfig.getUpdatedTemplate()).isNotEmpty();
	}
	
	private void assertPolicies(List<PolicyAgreementConfiguration> policyAgreements, List<Long> selected)
	{
		for (PolicyAgreementConfiguration policyAgreementConfiguration : policyAgreements)
		{
			assertThat(policyAgreementConfiguration.documentsIdsToAccept.get(0))
					.isEqualTo(selected.get(policyAgreements.indexOf(policyAgreementConfiguration)));
		}
	}
}
