package io.imunity.upman.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.translation.ActionValidationException;
import pl.edu.icm.unity.engine.api.translation.ExternalDataParser;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.translation.form.action.AddAttributeActionFactory;
import pl.edu.icm.unity.engine.translation.form.action.AddToGroupActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.types.translation.ProfileMode;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

@ExtendWith(MockitoExtension.class)

public class ProjectFormValidatorTest
{
	@Mock
	private RegistrationActionsRegistry registrationActionsRegistry;
	@Mock
	private GroupsManagement groupsManagement;

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
												"value")))))
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
														"/projects/A", "value")))))
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

}
