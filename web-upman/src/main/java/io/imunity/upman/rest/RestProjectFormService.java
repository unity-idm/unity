package io.imunity.upman.rest;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import io.imunity.rest.api.types.registration.RestEnquiryForm;
import io.imunity.rest.api.types.registration.RestRegistrationForm;
import io.imunity.upman.rest.ProjectFormsValidator.ProjectFormsValidatorFactory;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.rest.mappers.registration.EnquiryFormMapper;
import pl.edu.icm.unity.rest.mappers.registration.RegistrationFormMapper;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;

class RestProjectFormService
{
	private final UpmanRestAuthorizationManager authz;
	private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private final GroupsManagement groupMan;
	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;
	private final ProjectFormsValidator formValidator;

	private final String rootGroup;
	private final String authorizationGroup;

	RestProjectFormService(UpmanRestAuthorizationManager authz,
			GroupDelegationConfigGenerator groupDelegationConfigGenerator, GroupsManagement groupMan,
			RegistrationsManagement registrationsManagement, EnquiryManagement enquiryManagement,
			ProjectFormsValidator formValidator, String rootGroup, String authorizationGroup)
	{
		this.authz = authz;
		this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
		this.groupMan = groupMan;
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
		this.formValidator = formValidator;
		this.rootGroup = rootGroup;
		this.authorizationGroup = authorizationGroup;
	}

	void generateRegistrationForm(String projectId) throws EngineException
	{
		addRegistrationForm(projectId, Optional.empty());
	}

	void addRegistrationForm(String projectId, RestRegistrationForm form) throws EngineException
	{
		addRegistrationForm(projectId, Optional.of(form));
	}

	private void addRegistrationForm(String projectId, Optional<RestRegistrationForm> form) throws EngineException
	{
		assertAuthorization();

		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();

		if (groupDelegationConfiguration.registrationForm != null)
		{
			throw new IllegalArgumentException("Registration form for project " + projectId + " already exists");
		}

		RegistrationForm regForm = null;
		if (form.isPresent())
		{
			regForm = RegistrationFormMapper.map(form.get());
			formValidator.assertRegistrationForm(regForm, projectId);
		} else
		{
			regForm = groupDelegationConfigGenerator.generateProjectRegistrationForm(
					ProjectPathProvider.getProjectPath(projectId, rootGroup), groupDelegationConfiguration.logoUrl,
					groupDelegationConfiguration.attributes, groupDelegationConfiguration.policyDocumentsIds);
		}

		registrationsManagement.addForm(regForm);

		GroupDelegationConfiguration updatedGroupDelegationConfiguration = new GroupDelegationConfiguration(
				groupDelegationConfiguration.enabled, groupDelegationConfiguration.enableSubprojects,
				groupDelegationConfiguration.logoUrl, regForm.getName(), groupDelegationConfiguration.signupEnquiryForm,
				groupDelegationConfiguration.membershipUpdateEnquiryForm, groupDelegationConfiguration.attributes,
				groupDelegationConfiguration.policyDocumentsIds);
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(ProjectPathProvider.getProjectPath(projectId, rootGroup), group);
	}

	RestRegistrationForm getRegistrationForm(String projectId) throws EngineException
	{
		assertAuthorization();
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.registrationForm == null)
		{
			throw new IllegalArgumentException("Registration form for project " + projectId + " is undefined");
		}

		RegistrationForm form = registrationsManagement.getForm(groupDelegationConfiguration.registrationForm);
		return RegistrationFormMapper.map(form);
	}

	void removeRegistrationForm(String projectId) throws EngineException
	{
		assertAuthorization();
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.registrationForm == null)
		{
			throw new IllegalArgumentException("Registration form for project " + projectId + " is undefined");
		}

		GroupDelegationConfiguration updatedGroupDelegationConfiguration = new GroupDelegationConfiguration(
				groupDelegationConfiguration.enabled, groupDelegationConfiguration.enableSubprojects,
				groupDelegationConfiguration.logoUrl, null, groupDelegationConfiguration.signupEnquiryForm,
				groupDelegationConfiguration.membershipUpdateEnquiryForm, groupDelegationConfiguration.attributes,
				groupDelegationConfiguration.policyDocumentsIds);
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(ProjectPathProvider.getProjectPath(projectId, rootGroup), group);
		registrationsManagement.removeForm(groupDelegationConfiguration.registrationForm, true);
	}

	void updateRegistrationForm(String projectId, RestRegistrationForm registrationForm) throws EngineException
	{
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.registrationForm == null)
		{
			throw new IllegalArgumentException("Registration form for project " + projectId + " is undefined");
		}

		if (!registrationForm.name.equals(groupDelegationConfiguration.registrationForm))
		{
			throw new IllegalArgumentException("Can not update registration form name");

		}
		RegistrationForm regForm = RegistrationFormMapper.map(registrationForm);
		formValidator.assertRegistrationForm(regForm, projectId);
		registrationsManagement.updateForm(regForm, true);
	}

	void generateSignupEnquiryForm(String projectId) throws EngineException
	{
		addSignupEnquiryForm(projectId, Optional.empty());
	}

	void addSignupEnquiryForm(String projectId, RestEnquiryForm form) throws EngineException
	{
		addSignupEnquiryForm(projectId, Optional.of(form));
	}

	private void addSignupEnquiryForm(String projectId, Optional<RestEnquiryForm> form) throws EngineException
	{
		assertAuthorization();

		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.signupEnquiryForm != null)
		{
			throw new IllegalArgumentException("Signup enquiry form for project " + projectId + " already exists");
		}

		EnquiryForm enquiryForm = null;
		if (form.isPresent())
		{
			enquiryForm = EnquiryFormMapper.map(form.get());
			formValidator.assertCommonForm(enquiryForm, projectId);
		} else
		{
			enquiryForm = groupDelegationConfigGenerator.generateProjectJoinEnquiryForm(
					ProjectPathProvider.getProjectPath(projectId, rootGroup), groupDelegationConfiguration.logoUrl,
					groupDelegationConfiguration.policyDocumentsIds);
		}

		enquiryManagement.addEnquiry(enquiryForm);
		GroupDelegationConfiguration updatedGroupDelegationConfiguration = new GroupDelegationConfiguration(
				groupDelegationConfiguration.enabled, groupDelegationConfiguration.enableSubprojects,
				groupDelegationConfiguration.logoUrl, groupDelegationConfiguration.registrationForm,
				enquiryForm.getName(), groupDelegationConfiguration.membershipUpdateEnquiryForm,
				groupDelegationConfiguration.attributes, groupDelegationConfiguration.policyDocumentsIds);
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(ProjectPathProvider.getProjectPath(projectId, rootGroup), group);

	}

	RestEnquiryForm getSignupEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization();
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.signupEnquiryForm == null)
		{
			throw new IllegalArgumentException("Signup enquiry form for project " + projectId + " is undefined");
		}

		EnquiryForm form = enquiryManagement.getEnquiry(groupDelegationConfiguration.signupEnquiryForm);
		return EnquiryFormMapper.map(form);
	}

	void removeSignupEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization();
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.signupEnquiryForm == null)
		{
			throw new IllegalArgumentException("Signup enquiry form for project " + projectId + " is undefined");
		}

		GroupDelegationConfiguration updatedGroupDelegationConfiguration = new GroupDelegationConfiguration(
				groupDelegationConfiguration.enabled, groupDelegationConfiguration.enableSubprojects,
				groupDelegationConfiguration.logoUrl, groupDelegationConfiguration.registrationForm, null,
				groupDelegationConfiguration.membershipUpdateEnquiryForm, groupDelegationConfiguration.attributes,
				groupDelegationConfiguration.policyDocumentsIds);
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(ProjectPathProvider.getProjectPath(projectId, rootGroup), group);
		enquiryManagement.removeEnquiry(groupDelegationConfiguration.signupEnquiryForm, true);

	}

	void updateSignupEnquiryForm(String projectId, RestEnquiryForm restEnquiryForm) throws EngineException
	{
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.signupEnquiryForm == null)
		{
			throw new IllegalArgumentException("Signup enquiry form for project " + projectId + " is undefined");
		}

		if (!restEnquiryForm.name.equals(groupDelegationConfiguration.signupEnquiryForm))
		{
			throw new IllegalArgumentException("Can not update signup enquiry form name");

		}
		EnquiryForm enquiryForm = EnquiryFormMapper.map(restEnquiryForm);
		formValidator.assertCommonForm(enquiryForm, projectId);
		enquiryManagement.updateEnquiry(enquiryForm, true);
	}

	void generateMembershipUpdateEnquiryForm(String projectId) throws EngineException
	{
		addMembershipUpdateEnquiryForm(projectId, Optional.empty());
	}

	void addMembershipUpdateEnquiryForm(String projectId, RestEnquiryForm form) throws EngineException
	{
		addMembershipUpdateEnquiryForm(projectId, Optional.of(form));
	}

	private void addMembershipUpdateEnquiryForm(String projectId, Optional<RestEnquiryForm> form) throws EngineException
	{
		assertAuthorization();

		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.signupEnquiryForm != null)
		{
			throw new IllegalArgumentException(
					"Membership update enquiry form for project " + projectId + " already exists");
		}

		EnquiryForm enquiryForm = null;
		if (form.isPresent())
		{
			enquiryForm = EnquiryFormMapper.map(form.get());
			formValidator.assertCommonForm(enquiryForm, projectId);
		} else
		{
			enquiryForm = groupDelegationConfigGenerator.generateProjectJoinEnquiryForm(
					ProjectPathProvider.getProjectPath(projectId, rootGroup), groupDelegationConfiguration.logoUrl,
					groupDelegationConfiguration.policyDocumentsIds);
		}

		enquiryManagement.addEnquiry(enquiryForm);
		GroupDelegationConfiguration updatedGroupDelegationConfiguration = new GroupDelegationConfiguration(
				groupDelegationConfiguration.enabled, groupDelegationConfiguration.enableSubprojects,
				groupDelegationConfiguration.logoUrl, groupDelegationConfiguration.registrationForm,
				groupDelegationConfiguration.signupEnquiryForm, enquiryForm.getName(),
				groupDelegationConfiguration.attributes, groupDelegationConfiguration.policyDocumentsIds);
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(ProjectPathProvider.getProjectPath(projectId, rootGroup), group);
	}

	RestEnquiryForm getMembershipUpdateEnquiryForm(String projectId) throws EngineException
	{
		assertAuthorization();
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();

		if (groupDelegationConfiguration.membershipUpdateEnquiryForm == null)
		{
			throw new IllegalArgumentException("Membership update form for project " + projectId + " is undefined");
		}

		EnquiryForm form = enquiryManagement.getEnquiry(groupDelegationConfiguration.membershipUpdateEnquiryForm);
		return EnquiryFormMapper.map(form);
	}

	void removeMembershipUpdateEnquiryForm(String projectId) throws EngineException
	{

		assertAuthorization();
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.membershipUpdateEnquiryForm == null)
		{
			throw new IllegalArgumentException(
					"Membership update enquiry form for project " + projectId + " is undefined");
		}

		GroupDelegationConfiguration updatedGroupDelegationConfiguration = new GroupDelegationConfiguration(
				groupDelegationConfiguration.enabled, groupDelegationConfiguration.enableSubprojects,
				groupDelegationConfiguration.logoUrl, groupDelegationConfiguration.registrationForm,
				groupDelegationConfiguration.signupEnquiryForm, null, groupDelegationConfiguration.attributes,
				groupDelegationConfiguration.policyDocumentsIds);
		group.setDelegationConfiguration(updatedGroupDelegationConfiguration);
		groupMan.updateGroup(ProjectPathProvider.getProjectPath(projectId, rootGroup), group);
		enquiryManagement.removeEnquiry(groupDelegationConfiguration.membershipUpdateEnquiryForm, true);

	}

	void updateMembershipUpdateEnquiryForm(String projectId, RestEnquiryForm restEnquiryForm)
			throws EngineException
	{
		GroupContents groupContent = groupMan.getContents(ProjectPathProvider.getProjectPath(projectId, rootGroup),
				GroupContents.METADATA);
		Group group = groupContent.getGroup();
		GroupDelegationConfiguration groupDelegationConfiguration = group.getDelegationConfiguration();
		if (groupDelegationConfiguration.membershipUpdateEnquiryForm == null)
		{
			throw new IllegalArgumentException(
					"Membership update enquiry form for project " + projectId + " is undefined");
		}

		if (!restEnquiryForm.name.equals(groupDelegationConfiguration.membershipUpdateEnquiryForm))
		{
			throw new IllegalArgumentException("Can not update enquiry form name");

		}
		EnquiryForm enquiryForm = EnquiryFormMapper.map(restEnquiryForm);
		formValidator.assertCommonForm(enquiryForm, projectId);
		enquiryManagement.updateEnquiry(enquiryForm, true);
	}

	private void assertAuthorization() throws AuthorizationException
	{
		authz.assertManagerAuthorization(authorizationGroup);
	}

	@Component
	public static class RestProjectFormServiceFactory
	{
		private final UpmanRestAuthorizationManager authz;
		private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
		private final GroupsManagement groupMan;
		private final RegistrationsManagement registrationsManagement;
		private final EnquiryManagement enquiryManagement;
		private final ProjectFormsValidatorFactory projectFormsValidatorFactory;

		@Autowired
		RestProjectFormServiceFactory(UpmanRestAuthorizationManager authz,
				@Qualifier("insecure") GroupDelegationConfigGenerator groupDelegationConfigGenerator,
				@Qualifier("insecure") GroupsManagement groupMan,
				@Qualifier("insecure") RegistrationsManagement registrationsManagement,
				@Qualifier("insecure") EnquiryManagement enquiryManagement,
				ProjectFormsValidatorFactory projectFormsValidatorFactory)
		{
			this.authz = authz;
			this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
			this.groupMan = groupMan;
			this.registrationsManagement = registrationsManagement;
			this.enquiryManagement = enquiryManagement;
			this.projectFormsValidatorFactory = projectFormsValidatorFactory;
		}

		RestProjectFormService newInstance(String rootGroup, String authorizeGroup, List<String> rootGroupAttributes)
		{
			return new RestProjectFormService(authz, groupDelegationConfigGenerator, groupMan, registrationsManagement,
					enquiryManagement, projectFormsValidatorFactory.newInstance(rootGroup, rootGroupAttributes),
					rootGroup, authorizeGroup);
		}
	}
}
