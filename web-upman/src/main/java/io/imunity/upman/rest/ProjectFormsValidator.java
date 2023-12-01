package io.imunity.upman.rest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.api.translation.form.GroupRestrictedFormValidationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationActionFactory;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseFormNotifications;
import pl.edu.icm.unity.types.registration.GroupRegistrationParam;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.translation.TranslationAction;
import pl.edu.icm.unity.types.translation.TranslationProfile;
import pl.edu.icm.unity.types.translation.TranslationRule;

class ProjectFormsValidator
{
	private final RegistrationActionsRegistry registrationActionsRegistry;
	private final GroupsManagement groupMan;
	private final List<String> rootGroupAttributes;

	public ProjectFormsValidator(RegistrationActionsRegistry registrationActionsRegistry, GroupsManagement groupMan, List<String> rootGroupAttributes)
	{
		this.registrationActionsRegistry = registrationActionsRegistry;
		this.groupMan = groupMan;
		this.rootGroupAttributes = rootGroupAttributes;
	}

	public void assertRegistrationFormIsRestrictedToProjectGroup(RegistrationForm registrationForm, String projectPath)
			throws IllegalArgumentException, EngineException
	{
		assertCommonPartOfFormIsRestrictedToProjectGroup(registrationForm, projectPath);
		assertAutoLoginToRealm(registrationForm.getAutoLoginToRealm());
	}
	
	public void assertCommonPartOfFormIsRestrictedToProjectGroup(BaseForm form, String projectPath) throws EngineException
	{
		assertAdminGroup(form.getNotificationsConfiguration(), projectPath);
		assertGroups(form.getGroupParams(), projectPath);
		assertAttributes(form.getAttributeParams(), projectPath);
		assertFormAutomation(form.getTranslationProfile(), projectPath);
	}

	private void assertFormAutomation(TranslationProfile translationProfile, String projectPath) throws EngineException
	{
		for (TranslationRule rule : translationProfile.getRules())
		{
			validateAction(rule.getAction(), projectPath);
		}
	}

	private void validateAction(TranslationAction action, String projectPath) throws EngineException
	{
		RegistrationTranslationActionFactory factory = registrationActionsRegistry.getByName(action.getName());
		RegistrationTranslationAction instance = factory.getInstance(action.getParameters());
		instance.validateGroupRestrictedForm(GroupRestrictedFormValidationContext.builder()
				.withAllowedGroupWithChildren(projectPath)
				.withAllowedRootGroupAttributes(
						rootGroupAttributes)
				.build());
	}

	private void assertAttributes(List<AttributeRegistrationParam> attributeParams, String projectPath)
			throws IllegalArgumentException
	{

		if (attributeParams == null)
			return;
		for (AttributeRegistrationParam attributeRegistrationParam : attributeParams)
		{
			if (attributeRegistrationParam.getGroup()
					.equals("/"))
			{
				if (rootGroupAttributes.contains(attributeRegistrationParam.getAttributeType()))
				{
					continue;
				} else
				{
					throw new IllegalArgumentException("Attribute registration param is not permited in root group");
				}
			}

			if (!Group.isChildOrSame(attributeRegistrationParam.getGroup(), projectPath))
			{
				throw new IllegalArgumentException("Attribute registration param is out of the project scope");
			}
		}
	}

	private void assertGroups(List<GroupRegistrationParam> groupParams, String projectPath)
			throws EngineException, IllegalArgumentException
	{
		if (groupParams == null)
			return;
		Map<String, Group> allGroups = groupMan.getAllGroups();

		for (GroupRegistrationParam groupRegistrationParam : groupParams)
		{
			List<Group> filteredGroup = GroupPatternMatcher.filterMatching(allGroups.values()
					.stream()
					.collect(Collectors.toList()), groupRegistrationParam.getGroupPath());
			assertGroupsIsProjectGroups(filteredGroup, projectPath);
		}
	}

	private void assertGroupsIsProjectGroups(List<Group> filteredGroup, String projectPath)
			throws IllegalArgumentException
	{

		for (Group group : filteredGroup)
		{
			if (!Group.isChildOrSame(group.toString(), projectPath))
			{
				throw new IllegalArgumentException("Group registration param is out of the project scope");
			}
		}
	}

	private void assertAutoLoginToRealm(String autoLoginToRealm) throws IllegalArgumentException
	{
		if (autoLoginToRealm != null)
		{
			throw new IllegalArgumentException("Auto login to realm must be unset");

		}
	}

	private void assertAdminGroup(BaseFormNotifications notificationsConfiguration, String projectPath)
			throws IllegalArgumentException
	{
		if (notificationsConfiguration.getAdminsNotificationGroup() == null)
			return;

		if (!Group.isChildOrSame(notificationsConfiguration.getAdminsNotificationGroup(), projectPath))
		{
			throw new IllegalArgumentException("Group with administrators to be notified is out of the project scope");
		}
	}

	@Component
	public static class ProjectFormsValidatorFactory
	{
		private final GroupsManagement groupMan;
		private final RegistrationActionsRegistry registrationActionsRegistry;

		@Autowired
		ProjectFormsValidatorFactory(@Qualifier("insecure") GroupsManagement groupMan,

				RegistrationActionsRegistry registrationActionsRegistry)
		{

			this.groupMan = groupMan;
			this.registrationActionsRegistry = registrationActionsRegistry;
		}

		ProjectFormsValidator newInstance(List<String> rootGroupAttributes)
		{
			return new ProjectFormsValidator(registrationActionsRegistry, groupMan, rootGroupAttributes);
		}
	}
}
