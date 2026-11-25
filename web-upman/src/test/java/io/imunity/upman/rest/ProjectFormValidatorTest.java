package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.translation.ProfileMode;
import pl.edu.icm.unity.base.translation.ProfileType;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.identity.IdentityTypeSupport;
import pl.edu.icm.unity.engine.api.translation.ActionValidationException;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddIdentityActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;

@ExtendWith(MockitoExtension.class)
public class ProjectFormValidatorTest
{
	@Mock
	private RegistrationActionsRegistry registrationActionsRegistry;
	@Mock
	private GroupsManagement groupsManagement;

	@TempDir
	Path tempDir;

	private ProjectFormsValidator validator;

	@BeforeEach
	void setUp()
	{
		validator = new ProjectFormsValidator(registrationActionsRegistry, groupsManagement,
				List.of("attr1"));
	}

	@Test
	public void shouldThrowWhenGroupAdminToNotifiedIsOutOfTheProject() throws EngineException
	{
		RegistrationForm registrationForm = new RegistrationFormBuilder().withName("form")
				.withDefaultCredentialRequirement("cred")
				.withNotificationsConfiguration()
				.withAdminsNotificationGroup("/projects/B")
				.endNotificationsConfiguration()
				.build();

		Assertions.assertThrows(ProjectFormValidationException.class,
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"));

	}

	@Test
	public void shouldThrowWhenAutoLoginToRealmIsSet()  throws EngineException
	{
		RegistrationForm registrationForm = new RegistrationFormBuilder().withName("form")
				.withDefaultCredentialRequirement("cred")
				.withNotificationsConfiguration()
				.withAdminsNotificationGroup("/projects/A")
				.endNotificationsConfiguration()
				.withAutoLoginToRealm("realm")
				.build();
		Assertions.assertThrows(ProjectFormValidationException.class,
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"));
	}

	@Test
	public void shouldThrowWhenGroupParamIsOutOfTheProject() throws EngineException
	{
		RegistrationForm registrationForm = new RegistrationFormBuilder().withName("form")
				.withDefaultCredentialRequirement("cred")
				.withNotificationsConfiguration()
				.withAdminsNotificationGroup("/projects/A")
				.endNotificationsConfiguration()
				.withAddedGroupParam()
				.withGroupPath("/project/B")
				.endGroupParam()
				.build();
		when(groupsManagement.getAllGroups())
				.thenReturn(Map.of("/project/B", new Group("/project/B"), "/project/A", new Group("/project/A")));

		Assertions.assertThrows(ProjectFormValidationException.class,
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"));
	}

	@Test
	public void shouldThrowWhenAttributeParamIsOutOfTheProject() throws EngineException
	{
		RegistrationForm registrationForm = new RegistrationFormBuilder().withName("form")
				.withDefaultCredentialRequirement("cred")
				.withNotificationsConfiguration()
				.withAdminsNotificationGroup("/projects/A")
				.endNotificationsConfiguration()
				.withAddedAttributeParam()
				.withGroup("/project/B")
				.endAttributeParam()
				.build();

		Assertions.assertThrows(ProjectFormValidationException.class,
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"));
	}

	@Test
	public void shouldThrowWhenAttributeParamIsForbiddenInRootGroup() throws EngineException
	{
		RegistrationForm registrationForm = new RegistrationFormBuilder().withName("form")
				.withDefaultCredentialRequirement("cred")
				.withNotificationsConfiguration()
				.withAdminsNotificationGroup("/projects/A")
				.endNotificationsConfiguration()
				.withAddedAttributeParam()
				.withGroup("/")
				.withAttributeType("attr2")
				.endAttributeParam()
				.build();

		Assertions.assertThrows(ProjectFormValidationException.class,
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"));
	}

	@Test
	public void shouldThrowWhenAutomationAddAttributeActionValidationFailed()
			throws EngineException
	{
		RegistrationForm registrationForm = new RegistrationFormBuilder().withName("form")
				.withDefaultCredentialRequirement("cred")
				.withNotificationsConfiguration()
				.withAdminsNotificationGroup("/projects/A")
				.endNotificationsConfiguration()
				.withTranslationProfile(
						new TranslationProfile("name", "desc", ProfileType.REGISTRATION, ProfileMode.DEFAULT,
								List.of(new TranslationRule("true",
										new TranslationAction(AddAttributeActionFactory.NAME, "attr2", "/projects/B",
												"'value'")))))
				.build();
		AttributeTypeSupport attributeTypeSupport = mock(AttributeTypeSupport.class);
		when(attributeTypeSupport.getType("attr2")).thenReturn(new AttributeType("attr2", null));
		when(registrationActionsRegistry.getByName(AddAttributeActionFactory.NAME))
				.thenReturn(new AddAttributeActionFactory(attributeTypeSupport, mock(ExternalDataParser.class)));

		Assertions.assertThrows(ActionValidationException.class,
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"));
	}

	@Test
	public void shouldThrowWhenAutomationAddToGroupActionValidationFailed()
			throws EngineException
	{
		RegistrationForm registrationForm = new RegistrationFormBuilder().withName("form")
				.withDefaultCredentialRequirement("cred")
				.withNotificationsConfiguration()
				.withAdminsNotificationGroup("/projects/A")
				.endNotificationsConfiguration()
				.withTranslationProfile(
						new TranslationProfile("name", "desc", ProfileType.REGISTRATION, ProfileMode.DEFAULT,
								List.of(new TranslationRule("true",
										new TranslationAction(AddToGroupActionFactory.NAME, "'/projects/B'")))))
				.build();

		when(registrationActionsRegistry.getByName(AddToGroupActionFactory.NAME))
				.thenReturn(new AddToGroupActionFactory());

		Assertions.assertThrows(ActionValidationException.class,
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"));
	}

	@Test
	public void shouldValidateForm() throws EngineException
	{
		RegistrationForm registrationForm = new RegistrationFormBuilder().withName("form")
				.withDefaultCredentialRequirement("cred")
				.withNotificationsConfiguration()
				.withAdminsNotificationGroup("/projects/A")
				.endNotificationsConfiguration()
				.withAddedAttributeParam()
				.withGroup("/")
				.withAttributeType("attr1")
				.endAttributeParam()
				.withAddedGroupParam()
				.withGroupPath("/projects/A/C")
				.endGroupParam()
				.withTranslationProfile(
						new TranslationProfile("name", "desc", ProfileType.REGISTRATION, ProfileMode.DEFAULT,
								List.of(new TranslationRule("true",
										new TranslationAction(AddToGroupActionFactory.NAME, "'/projects/A'")),
										new TranslationRule("true",
												new TranslationAction(AddAttributeActionFactory.NAME, "attr2",
														"/projects/A", "'value'")))))
				.build();

		when(registrationActionsRegistry.getByName(AddToGroupActionFactory.NAME))
				.thenReturn(new AddToGroupActionFactory());
		AttributeTypeSupport attributeTypeSupport = mock(AttributeTypeSupport.class);
		when(attributeTypeSupport.getType("attr2")).thenReturn(new AttributeType("attr2", null));
		when(registrationActionsRegistry.getByName(AddAttributeActionFactory.NAME))
				.thenReturn(new AddAttributeActionFactory(attributeTypeSupport, mock(ExternalDataParser.class)));

		Assertions.assertDoesNotThrow(
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"));
	}

	@Test
	public void shouldRejectRuleConditionExecutingCode()
	{
		Path marker = tempDir.resolve("condition");
		RegistrationForm registrationForm = formWithAutomation(new TranslationRule(touchExpression(marker),
				new TranslationAction(AddToGroupActionFactory.NAME, "'/projects/A'")));

		assertThatThrownBy(
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"))
				.isInstanceOf(ProjectFormValidationException.class)
				.hasMessageContaining("Form automation rule condition");
		assertThat(marker).doesNotExist();
	}

	@Test
	public void shouldRejectAddAttributeExpressionBeforeCompilingIt()
	{
		Path marker = tempDir.resolve("addAttribute");
		RegistrationForm registrationForm = formWithAutomation(new TranslationRule("true",
				new TranslationAction(AddAttributeActionFactory.NAME, "attr2", "/projects/A",
						touchExpression(marker))));
		when(registrationActionsRegistry.getByName(AddAttributeActionFactory.NAME))
				.thenReturn(new AddAttributeActionFactory(mock(AttributeTypeSupport.class),
						mock(ExternalDataParser.class)));

		assertThatThrownBy(
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"))
				.isInstanceOf(ProjectFormValidationException.class)
				.hasMessageContaining("Expression of form automation action " + AddAttributeActionFactory.NAME);
		assertThat(marker).doesNotExist();
	}

	@Test
	public void shouldRejectAddIdentityExpressionExecutingCode()
	{
		RegistrationForm registrationForm = formWithAutomation(new TranslationRule("true",
				new TranslationAction(AddIdentityActionFactory.NAME, "userName", "new java.lang.Object()")));
		when(registrationActionsRegistry.getByName(AddIdentityActionFactory.NAME))
				.thenReturn(new AddIdentityActionFactory(mock(IdentityTypeSupport.class),
						mock(ExternalDataParser.class)));

		assertThatThrownBy(
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"))
				.isInstanceOf(ProjectFormValidationException.class)
				.hasMessageContaining("Expression of form automation action " + AddIdentityActionFactory.NAME);
	}

	@Test
	public void shouldRejectAddToGroupLiteralConcatenatedWithExpression()
	{
		RegistrationForm registrationForm = formWithAutomation(new TranslationRule("true",
				new TranslationAction(AddToGroupActionFactory.NAME, "'/projects/A/x' + '/../../B'")));
		when(registrationActionsRegistry.getByName(AddToGroupActionFactory.NAME))
				.thenReturn(new AddToGroupActionFactory());

		assertThatThrownBy(
				() -> validator.assertRegistrationFormIsRestrictedToProjectGroup(registrationForm, "/projects/A"))
				.isInstanceOf(ActionValidationException.class)
				.hasMessageContaining("Only literal expression is allowed");
	}

	@Test
	public void shouldRejectEnquiryTargetConditionExecutingCode()
	{
		Path marker = tempDir.resolve("targetCondition");
		EnquiryForm enquiryForm = new EnquiryFormBuilder().withName("enquiry")
				.withType(EnquiryForm.EnquiryType.STICKY)
				.withTargetGroups(new String[] { "/projects/A" })
				.withTargetCondition(touchExpression(marker))
				.build();

		assertThatThrownBy(() -> validator.assertEnquiryFormIsRestrictedToProjectGroup(enquiryForm, "/projects/A"))
				.isInstanceOf(ProjectFormValidationException.class)
				.hasMessageContaining("Enquiry target condition");
		assertThat(marker).doesNotExist();
	}

	@Test
	public void shouldAcceptEnquiryTargetConditionUsingEntityContext()
	{
		EnquiryForm enquiryForm = new EnquiryFormBuilder().withName("enquiry")
				.withType(EnquiryForm.EnquiryType.STICKY)
				.withTargetGroups(new String[] { "/projects/A" })
				.withTargetCondition("credStatus['password'] == 'correct' && groups contains '/projects/A'")
				.build();

		Assertions.assertDoesNotThrow(
				() -> validator.assertEnquiryFormIsRestrictedToProjectGroup(enquiryForm, "/projects/A"));
	}

	private static RegistrationForm formWithAutomation(TranslationRule rule)
	{
		return new RegistrationFormBuilder().withName("form")
				.withDefaultCredentialRequirement("cred")
				.withNotificationsConfiguration()
				.withAdminsNotificationGroup("/projects/A")
				.endNotificationsConfiguration()
				.withTranslationProfile(new TranslationProfile("name", "desc", ProfileType.REGISTRATION,
						ProfileMode.DEFAULT, List.of(rule)))
				.build();
	}

	private static String touchExpression(Path marker)
	{
		return "java.lang.Runtime.getRuntime().exec('touch " + marker + "') != null";
	}
}
