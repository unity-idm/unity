package io.imunity.upman.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.rest.mappers.registration.EnquiryFormMapper;
import io.imunity.rest.mappers.registration.RegistrationFormMapper;
import io.imunity.upman.rest.ProjectFormsValidator.ProjectFormsValidatorFactory;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;

class RestProjectFormServiceNoAuthz
{
	private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private final GroupsManagement groupMan;
	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;
	private final ProjectFormsValidator formValidator;
	private final ProjectGroupProvider projectGroupProvider;
	
	private final String rootGroup;

	RestProjectFormServiceNoAuthz(GroupDelegationConfigGenerator groupDelegationConfigGenerator,
			GroupsManagement groupMan, RegistrationsManagement registrationsManagement,
			EnquiryManagement enquiryManagement, ProjectFormsValidator formValidator, String rootGroup)
	{
		this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
		this.groupMan = groupMan;
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
		this.formValidator = formValidator;
		this.rootGroup = rootGroup;
		this.projectGroupProvider = new ProjectGroupProvider(groupMan);
	}

	void generateRegistrationForm(String projectId) throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		assertRegistrationFormIsProvided(projectPath, group);
		RegistrationForm regForm = groupDelegationConfigGenerator.generateProjectRegistrationForm(projectPath,
				groupDelegationConfiguration.logoUrl, groupDelegationConfiguration.attributes,
				groupDelegationConfiguration.policyDocumentsIds);
		addRegistratioFormAndUpdateGroupConfig(group, regForm);
	}

	void addRegistrationForm(String projectId, RestRegistrationForm form) throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		assertRegistrationFormIsProvided(projectId, group);
		RegistrationForm regForm = RegistrationFormMapper.map(form);
		formValidator.assertRegistrationFormIsRestrictedToProjectGroup(regForm, projectPath);
		addRegistratioFormAndUpdateGroupConfig(group, regForm);
	}

	private void addRegistratioFormAndUpdateGroupConfig(Group group, RegistrationForm regForm) throws EngineException
	{
		registrationsManagement.addForm(regForm);
		GroupDelegationConfiguration updatedGroupDelegationConfiguration = GroupDelegationConfiguration.builder()
				.copy(group.getDelegationConfiguration())
				.withRegistrationForm(regForm.getName())
				.build();
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(group.toString(), group);
	}

	private void assertRegistrationFormIsProvided(String projectId, Group group)
	{
		if (group.getDelegationConfiguration().registrationForm != null)
		{
			throw new IllegalArgumentException("Registration form for project " + projectId + " already exists");
		}
	}

	RestRegistrationForm getRegistrationForm(String projectId) throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.registrationForm == null)
		{
			throw new ProjectFormNotFoundException("Registration form for project " + projectId + " is undefined");
		}

		RegistrationForm form = registrationsManagement.getForm(groupDelegationConfiguration.registrationForm);
		return RegistrationFormMapper.map(form);
	}

	void removeRegistrationForm(String projectId, boolean dropRequests) throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.registrationForm == null)
		{
			throw new ProjectFormNotFoundException("Registration form for project " + projectId + " is undefined");
		}

		GroupDelegationConfiguration updatedGroupDelegationConfiguration = GroupDelegationConfiguration.builder()
				.copy(groupDelegationConfiguration)
				.withRegistrationForm(null)
				.build();
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(projectPath, group);
		registrationsManagement.removeForm(groupDelegationConfiguration.registrationForm, dropRequests);
	}

	void updateRegistrationForm(String projectId, RestRegistrationForm registrationForm, boolean ignoreRequests)
			throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.registrationForm == null)
		{
			throw new ProjectFormNotFoundException("Registration form for project " + projectId + " is undefined");
		}

		if (!registrationForm.name.equals(groupDelegationConfiguration.registrationForm))
		{
			throw new IllegalArgumentException(
					"Can not update registration form name to " + registrationForm.name + " for project " + projectId);

		}
		RegistrationForm regForm = RegistrationFormMapper.map(registrationForm);
		formValidator.assertRegistrationFormIsRestrictedToProjectGroup(regForm, projectPath);
		registrationsManagement.updateForm(regForm, ignoreRequests);
	}

	void generateSignupEnquiryForm(String projectId) throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		assertSignupEnquiryIsProvided(projectId, group);
		GroupDelegationConfiguration delegationConfiguration = group.getDelegationConfiguration();
		EnquiryForm enquiryForm = groupDelegationConfigGenerator.generateProjectJoinEnquiryForm(
				ProjectPathProvider.getProjectPath(projectId, rootGroup), delegationConfiguration.logoUrl,
				delegationConfiguration.policyDocumentsIds);
		addSignupEnquiryFormAndUpdateGroupConfig(group, enquiryForm);
	}

	void addSignupEnquiryForm(String projectId, RestEnquiryForm form) throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		assertSignupEnquiryIsProvided(projectId, group);
		EnquiryForm enquiryForm = EnquiryFormMapper.map(form);
		formValidator.assertCommonPartOfFormIsRestrictedToProjectGroup(enquiryForm, projectPath);
		addSignupEnquiryFormAndUpdateGroupConfig(group, enquiryForm);
	}

	private void addSignupEnquiryFormAndUpdateGroupConfig(Group group, EnquiryForm enquiryForm) throws EngineException
	{
		enquiryManagement.addEnquiry(enquiryForm);
		GroupDelegationConfiguration updatedGroupDelegationConfiguration = GroupDelegationConfiguration.builder()
				.copy(group.getDelegationConfiguration())
				.withSignupEnquiryForm(enquiryForm.getName())
				.build();
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(group.toString(), group);
	}

	private void assertSignupEnquiryIsProvided(String projectId, Group group)
	{
		if (group.getDelegationConfiguration().signupEnquiryForm != null)
		{
			throw new IllegalArgumentException("Signup enquiry form for project " + projectId + " already exists");
		}
	}

	RestEnquiryForm getSignupEnquiryForm(String projectId) throws EngineException
	{
		Group group = projectGroupProvider.getProjectGroup(projectId, ProjectPathProvider.getProjectPath(projectId, rootGroup));
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.signupEnquiryForm == null)
		{
			throw new ProjectFormNotFoundException("Signup enquiry form for project " + projectId + " is undefined");
		}

		EnquiryForm form = enquiryManagement.getEnquiry(groupDelegationConfiguration.signupEnquiryForm);
		return EnquiryFormMapper.map(form);
	}

	void removeSignupEnquiryForm(String projectId, boolean dropRequests) throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.signupEnquiryForm == null)
		{
			throw new ProjectFormNotFoundException("Signup enquiry form for project " + projectId + " is undefined");
		}

		GroupDelegationConfiguration updatedGroupDelegationConfiguration = GroupDelegationConfiguration.builder()
				.copy(groupDelegationConfiguration)
				.withSignupEnquiryForm(null)
				.build();
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(projectPath, group);
		enquiryManagement.removeEnquiry(groupDelegationConfiguration.signupEnquiryForm, dropRequests);

	}

	void updateSignupEnquiryForm(String projectId, RestEnquiryForm restEnquiryForm, boolean ignoreRequests)
			throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.signupEnquiryForm == null)
		{
			throw new ProjectFormNotFoundException("Signup enquiry form for project " + projectId + " is undefined");
		}

		if (!restEnquiryForm.name.equals(groupDelegationConfiguration.signupEnquiryForm))
		{
			throw new IllegalArgumentException(
					"Can not update signup enquiry form name to " + restEnquiryForm.name + " for project " + projectId);

		}
		EnquiryForm enquiryForm = EnquiryFormMapper.map(restEnquiryForm);
		formValidator.assertCommonPartOfFormIsRestrictedToProjectGroup(enquiryForm, projectPath);
		enquiryManagement.updateEnquiry(enquiryForm, ignoreRequests);
	}

	void generateMembershipUpdateEnquiryForm(String projectId) throws EngineException
	{
		Group group = projectGroupProvider.getProjectGroup(projectId, ProjectPathProvider.getProjectPath(projectId, rootGroup));
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		assertMembershipUpdateIsProvided(projectId, group);

		EnquiryForm enquiryForm = groupDelegationConfigGenerator.generateProjectUpdateEnquiryForm(
				ProjectPathProvider.getProjectPath(projectId, rootGroup), groupDelegationConfiguration.logoUrl);

		addMembershipUpdateFormAndUpdateGroupConfig(group, enquiryForm);
	}

	void addMembershipUpdateEnquiryForm(String projectId, RestEnquiryForm form) throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		assertMembershipUpdateIsProvided(projectId, group);
		EnquiryForm enquiryForm = EnquiryFormMapper.map(form);
		formValidator.assertCommonPartOfFormIsRestrictedToProjectGroup(enquiryForm, projectPath);
		addMembershipUpdateFormAndUpdateGroupConfig(group, enquiryForm);
	}

	private void addMembershipUpdateFormAndUpdateGroupConfig(Group group, EnquiryForm enquiryForm)
			throws EngineException
	{
		enquiryManagement.addEnquiry(enquiryForm);
		GroupDelegationConfiguration updatedGroupDelegationConfiguration = GroupDelegationConfiguration.builder()
				.copy(group.getDelegationConfiguration())
				.withMembershipUpdateEnquiryForm(enquiryForm.getName())
				.build();
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(group.toString(), group);
	}

	private void assertMembershipUpdateIsProvided(String projectId, Group group)
	{
		if (group.getDelegationConfiguration().membershipUpdateEnquiryForm != null)
		{
			throw new IllegalArgumentException(
					"Membership update enquiry form for project " + projectId + " already exists");
		}
	}

	RestEnquiryForm getMembershipUpdateEnquiryForm(String projectId) throws EngineException
	{
		Group group = projectGroupProvider.getProjectGroup(projectId, ProjectPathProvider.getProjectPath(projectId, rootGroup));
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();

		if (groupDelegationConfiguration.membershipUpdateEnquiryForm == null)
		{
			throw new ProjectFormNotFoundException("Membership update form for project " + projectId + " is undefined");
		}

		EnquiryForm form = enquiryManagement.getEnquiry(groupDelegationConfiguration.membershipUpdateEnquiryForm);
		return EnquiryFormMapper.map(form);
	}

	void removeMembershipUpdateEnquiryForm(String projectId, boolean dropRequests) throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.membershipUpdateEnquiryForm == null)
		{
			throw new ProjectFormNotFoundException(
					"Membership update enquiry form for project " + projectId + " is undefined");
		}

		GroupDelegationConfiguration updatedGroupDelegationConfiguration = GroupDelegationConfiguration.builder()
				.copy(groupDelegationConfiguration)
				.withMembershipUpdateEnquiryForm(null)
				.build();
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(projectPath, group);
		enquiryManagement.removeEnquiry(groupDelegationConfiguration.membershipUpdateEnquiryForm, dropRequests);

	}

	void updateMembershipUpdateEnquiryForm(String projectId, RestEnquiryForm restEnquiryForm, boolean ignoreRequests)
			throws EngineException
	{
		String projectPath = ProjectPathProvider.getProjectPath(projectId, rootGroup);
		Group group = projectGroupProvider.getProjectGroup(projectId, projectPath);
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.membershipUpdateEnquiryForm == null)
		{
			throw new ProjectFormNotFoundException(
					"Membership update enquiry form for project " + projectId + " is undefined");
		}

		if (!restEnquiryForm.name.equals(groupDelegationConfiguration.membershipUpdateEnquiryForm))
		{
			throw new IllegalArgumentException("Can not update embership update enquiry form name to "
					+ restEnquiryForm.name + " for project " + projectId);

		}
		EnquiryForm enquiryForm = EnquiryFormMapper.map(restEnquiryForm);
		formValidator.assertCommonPartOfFormIsRestrictedToProjectGroup(enquiryForm, projectPath);
		enquiryManagement.updateEnquiry(enquiryForm, ignoreRequests);
	}

	@Component
	public static class RestProjectFormServiceNoAuthzFactory
	{
		private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
		private final GroupsManagement groupMan;
		private final RegistrationsManagement registrationsManagement;
		private final EnquiryManagement enquiryManagement;
		private final ProjectFormsValidatorFactory projectFormsValidatorFactory;

		@Autowired
		RestProjectFormServiceNoAuthzFactory(
				@Qualifier("insecure") GroupDelegationConfigGenerator groupDelegationConfigGenerator,
				@Qualifier("insecure") GroupsManagement groupMan,
				@Qualifier("insecure") RegistrationsManagement registrationsManagement,
				@Qualifier("insecure") EnquiryManagement enquiryManagement,
				ProjectFormsValidatorFactory projectFormsValidatorFactory)
		{
			this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
			this.groupMan = groupMan;
			this.registrationsManagement = registrationsManagement;
			this.enquiryManagement = enquiryManagement;
			this.projectFormsValidatorFactory = projectFormsValidatorFactory;
		}

		RestProjectFormServiceNoAuthz newInstance(String rootGroup, List<String> rootGroupAttributes)
		{
			return new RestProjectFormServiceNoAuthz(groupDelegationConfigGenerator, groupMan, registrationsManagement,
					enquiryManagement, projectFormsValidatorFactory.newInstance(rootGroupAttributes), rootGroup);
		}
	}
}
