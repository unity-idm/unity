package io.imunity.upman.rest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.registration.AttributeRegistrationParam;
import pl.edu.icm.unity.base.registration.BaseForm;
import pl.edu.icm.unity.base.registration.BaseFormNotifications;
import pl.edu.icm.unity.base.registration.GroupRegistrationParam;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.registration.GroupPatternMatcher;
import pl.edu.icm.unity.engine.api.translation.ActionValidationException;
import pl.edu.icm.unity.engine.api.translation.form.GroupRestrictedFormValidationContext;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationActionsRegistry;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationAction;
import pl.edu.icm.unity.engine.api.translation.form.RegistrationTranslationActionFactory;

class ProjectFormsValidator
{
	private final RegistrationActionsRegistry registrationActionsRegistry;
	private final GroupsManagement groupMan;
	private final List<String> rootGroupAttributes;

	public ProjectFormsValidator(RegistrationActionsRegistry registrationActionsRegistry, GroupsManagement groupMan,
			List<String> rootGroupAttributes)
	{
		this.registrationActionsRegistry = registrationActionsRegistry;
		this.groupMan = groupMan;
		this.rootGroupAttributes = rootGroupAttributes;
	}

	public void assertRegistrationFormIsRestrictedToProjectGroup(RegistrationForm registrationForm, String projectPath)
			throws ProjectFormValidationException, ActionValidationException, EngineException
	{
		assertCommonPartOfFormIsRestrictedToProjectGroup(registrationForm, projectPath);
		assertAutoLoginToRealm(registrationForm.getAutoLoginToRealm());
	}

	public void assertCommonPartOfFormIsRestrictedToProjectGroup(BaseForm form, String projectPath)
			throws ProjectFormValidationException, ActionValidationException, EngineException
	{
		assertAdminGroup(form.getNotificationsConfiguration(), projectPath);
		assertGroups(form.getGroupParams(), projectPath);
		assertAttributes(form.getAttributeParams(), projectPath);
		assertFormAutomation(form.getTranslationProfile(), projectPath);
	}

	private void assertFormAutomation(TranslationProfile translationProfile, String projectPath)
			throws EngineException, ActionValidationException
	{
		for (TranslationRule rule : translationProfile.getRules())
		{
			validateAction(rule.getAction(), projectPath);
		}
	}

	private void validateAction(TranslationAction action, String projectPath)
			throws ActionValidationException, EngineException
	{
		RegistrationTranslationActionFactory factory = registrationActionsRegistry.getByName(action.getName());
		RegistrationTranslationAction instance = factory.getInstance(action.getParameters());
		instance.validateGroupRestrictedForm(GroupRestrictedFormValidationContext.builder()
				.withParentGroup(projectPath)
				.withAllowedRootGroupAttributes(rootGroupAttributes)
				.build());
	}

	private void assertAttributes(List<AttributeRegistrationParam> attributeParams, String projectPath)
			throws ProjectFormValidationException
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
					throw new ProjectFormValidationException("Attribute registration parameter "
							+ attributeRegistrationParam.getAttributeType() + " is not permited in root group");
				}
			}

			if (!Group.isChildOrSame(attributeRegistrationParam.getGroup(), projectPath))
			{
				throw new ProjectFormValidationException("Attribute registration parameter "
						+ attributeRegistrationParam.getAttributeType() + " is outside of the project’s root group");
			}
		}
	}

	private void assertGroups(List<GroupRegistrationParam> groupParams, String projectPath)
			throws EngineException, ProjectFormValidationException
	{
		if (groupParams == null)
			return;
		Map<String, Group> allGroups = groupMan.getAllGroups();

		for (GroupRegistrationParam groupRegistrationParam : groupParams)
		{
			List<Group> filteredGroup = GroupPatternMatcher.filterMatching(allGroups.values()
					.stream()
					.collect(Collectors.toList()), groupRegistrationParam.getGroupPath());
			assertGroupsIsProjectGroups(groupRegistrationParam.getGroupPath(), filteredGroup, projectPath);
		}
	}

	private void assertGroupsIsProjectGroups(String groupPath, List<Group> filteredGroup, String projectPath)
			throws ProjectFormValidationException
	{

		for (Group group : filteredGroup)
		{
			if (!Group.isChildOrSame(group.toString(), projectPath))
			{
				throw new ProjectFormValidationException("Group registration parameter " + groupPath + " is outside of the project’s root group");
			}
		}
	}

	private void assertAutoLoginToRealm(String autoLoginToRealm) throws ProjectFormValidationException
	{
		if (autoLoginToRealm != null && !autoLoginToRealm.isEmpty())
		{
			throw new ProjectFormValidationException("Auto login to realm must be unset");
		}
	}

	private void assertAdminGroup(BaseFormNotifications notificationsConfiguration, String projectPath)
			throws ProjectFormValidationException
	{
		if (notificationsConfiguration.getAdminsNotificationGroup() == null)
			return;

		if (!Group.isChildOrSame(notificationsConfiguration.getAdminsNotificationGroup(), projectPath))
		{
			throw new ProjectFormValidationException("Group with administrators to be notified "
					+ notificationsConfiguration.getAdminsNotificationGroup() + " is outside of the project’s root group");
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
