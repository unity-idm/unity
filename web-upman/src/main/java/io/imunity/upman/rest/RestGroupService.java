/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.RegistrationForm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
class RestGroupService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, RestGroupService.class);

	private final DelegatedGroupManagement delGroupMan;
	private final GroupsManagement groupMan;
	private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private final UpmanRestAuthorizationManager authz;
	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;

	private final String authorizationGroup;


	public RestGroupService(@Qualifier("insecure") DelegatedGroupManagement delGroupMan,
	                        @Qualifier("insecure") GroupsManagement groupMan,
	                        @Qualifier("insecure") GroupDelegationConfigGenerator groupDelegationConfigGenerator,
	                        @Qualifier("insecure") RegistrationsManagement registrationsManagement,
	                        @Qualifier("insecure") EnquiryManagement enquiryManagement,
	                        UpmanRestAuthorizationManager authz)
	{
		this.delGroupMan = delGroupMan;
		this.groupMan = groupMan;
		this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
		this.authz = authz;
	}

	public void addProject(String rootGroup, RestProjectCreateRequest project) throws EngineException
	{
		assertAuthorization(rootGroup, project);

		GroupContents groupContent = groupMan.getContents(rootGroup, GroupContents.GROUPS);
		List<String> subGroups = groupContent.getSubGroups();

		if (project.displayedName == null || project.displayedName.isEmpty())
			throw new IllegalArgumentException();

		String name = generateName(subGroups);


		Group toAdd = new Group(new Group(project.groupName), name);
		toAdd.setPublic(project.isPublic);
		toAdd.setDisplayedName(convertToI18nString(project.displayedName));
		toAdd.setDescription(convertToI18nString(project.description));

		setDelegation(project.registrationForm, project.signUpEnquiry, project.membershipUpdateEnquiry,
			project.enableDelegation, project.registrationFormAutogenerate, rootGroup, project.groupName,
			project.logoUrl, project.signUpEnquiryAutogenerate, project.membershipUpdateEnquiryAutogenerate, toAdd,
			project.enableSubprojects, project.readOnlyAttributes);


		groupMan.addGroup(toAdd);
	}

	private void assertAuthorization(String rootGroup, RestProjectCreateRequest project) throws AuthorizationException
	{
		authz.assertManagerAuthorization(rootGroup, project.groupName, authorizationGroup);
	}

	public void updateProject(String rootGroup, String groupName, RestProjectUpdateRequest project) throws EngineException
	{
		authz.assertManagerAuthorization(authorizationGroup);

		GroupContents groupContent = groupMan.getContents(rootGroup, GroupContents.GROUPS);
		List<String> subGroups = groupContent.getSubGroups();

		if (project.displayedName == null || project.displayedName.isEmpty())
			throw new IllegalArgumentException();

		String name = generateName(subGroups);


		Group toUpdate = new Group(new Group(groupName), name);
		toUpdate.setPublic(project.isPublic);
		toUpdate.setDisplayedName(convertToI18nString(project.displayedName));
		toUpdate.setDescription(convertToI18nString(project.description));

		setDelegation(project.registrationForm, project.signUpEnquiry, project.membershipUpdateEnquiry,
			project.enableDelegation, project.registrationFormAutogenerate, rootGroup, groupName, project.logoUrl,
			project.signUpEnquiryAutogenerate, project.membershipUpdateEnquiryAutogenerate, toUpdate,
			project.enableSubprojects, project.readOnlyAttributes);


		groupMan.updateGroup(groupName, toUpdate);
	}

	private void setDelegation(String project, String project1, String project2, boolean project3, boolean project4,
	                           String rootGroup, String groupName, String project5, boolean project6, boolean project7, Group toAdd, boolean project8, List<String> project9) throws EngineException
	{
		String registrationFormName = project;
		String joinEnquiryName = project1;
		String updateEnquiryName = project2;

		if (project3)
		{
			if (project4)
			{
				RegistrationForm regForm = groupDelegationConfigGenerator
					.generateSubprojectRegistrationForm(project,
						rootGroup, groupName, project5);
				registrationsManagement.addForm(regForm);
				registrationFormName = regForm.getName();
			}

			if (project6)
			{
				EnquiryForm joinEnquiryForm = groupDelegationConfigGenerator
					.generateSubprojectJoinEnquiryForm(project1,
						rootGroup, groupName,
						project5);
				enquiryManagement.addEnquiry(joinEnquiryForm);
				joinEnquiryName = joinEnquiryForm.getName();

			}
			if (project7)
			{
				EnquiryForm updateEnquiryForm = groupDelegationConfigGenerator
					.generateSubprojectUpdateEnquiryForm(
						project2,
						rootGroup, groupName,
						project5);
				enquiryManagement.addEnquiry(updateEnquiryForm);
				updateEnquiryName = updateEnquiryForm.getName();
			}
		}

		toAdd.setDelegationConfiguration(new GroupDelegationConfiguration(project3,
			project8,
			project5,
			registrationFormName, joinEnquiryName, updateEnquiryName,
			project9));
	}

	private I18nString convertToI18nString(Map<String, String> map)
	{
		I18nString i18nString = new I18nString();
		i18nString.addAllValues(map);
		Optional.ofNullable(map.get("")).ifPresent(i18nString::setDefaultValue);
		return i18nString;
	}

	private static String generateName(List<String> subGroups)
	{
		String name;
		do
		{
			name = CodeGenerator.generateMixedCharCode(5);
		} while (subGroups.contains(name));
		return name;
	}

	public void removeProject(String rootPath, String groupName) throws EngineException
	{
		authz.assertManagerAuthorization(authorizationGroup);
		delGroupMan.removeGroup(rootPath, groupName);
	}

	public RestProject getProject(String rootPath, String groupName) throws EngineException
	{
		authz.assertManagerAuthorization(authorizationGroup);
		GroupContents orgGroupContents = groupMan.getContents(rootPath + groupName,
			GroupContents.GROUPS | GroupContents.METADATA);
		Group orgGroup = orgGroupContents.getGroup();
		return map(orgGroup);
	}

	public List<RestProject> getProjects(String rootPath) throws EngineException
	{
		authz.assertManagerAuthorization(authorizationGroup);
		List<Group> groups = groupMan.getGroupsByWildcard(rootPath + "*");
		return groups.stream()
			.map(RestGroupService::map)
			.collect(Collectors.toList());
	}

	public void addProjectMember(String rootPath, String groupName, long entityId) throws EngineException
	{
		authz.assertManagerAuthorization(authorizationGroup);
		delGroupMan.addMemberToGroup(rootPath, groupName, entityId);
	}

	public void removeProjectMember(String rootPath, String groupName, long entityId) throws EngineException
	{
		authz.assertManagerAuthorization(authorizationGroup);
		delGroupMan.removeMemberFromGroup(rootPath, groupName, entityId);
	}

	public List<RestProjectMembership> getProjectMembers(String rootPath, String groupName) throws EngineException
	{
		authz.assertManagerAuthorization(authorizationGroup);
		GroupContents orgGroupContents = groupMan.getContents(rootPath + groupName,
			GroupContents.GROUPS | GroupContents.METADATA);
		return orgGroupContents.getMembers().stream()
			.map(RestGroupService::map)
			.collect(Collectors.toList());
	}

	public RestProjectMembership getProjectMember(String rootPath, String groupName, long entityId) throws EngineException
	{
		authz.assertManagerAuthorization(authorizationGroup);
		GroupContents orgGroupContents = groupMan.getContents(rootPath + groupName,
			GroupContents.GROUPS | GroupContents.METADATA);
		return orgGroupContents.getMembers().stream()
			.filter(member -> member.getEntityId() == entityId)
			.map(RestGroupService::map)
			.findFirst()
			.orElseThrow(IllegalArgumentException::new);
	}

	public RestAuthorizationRole getProjectAuthorizationRole(String rootPath, String groupName, long entityId) throws EngineException
	{
		authz.assertManagerAuthorization(authorizationGroup);
		GroupAuthorizationRole groupAuthorizationRole = delGroupMan.getGroupAuthorizationRole(rootPath + groupName, entityId);
		return new RestAuthorizationRole(groupAuthorizationRole.name());
	}

	public void setProjectAuthorizationRole(String rootPath, String groupName, long entityId,
	                                                         RestAuthorizationRole role) throws EngineException
	{
		authz.assertManagerAuthorization(authorizationGroup);
		delGroupMan.setGroupAuthorizationRole(rootPath, groupName, entityId, GroupAuthorizationRole.valueOf(role.role));
	}

	private static RestProject map(Group orgGroup)
	{
		return RestProject.builder()
			.withGroupName(orgGroup.getName())
			.withIsPublic(orgGroup.isPublic())
			.withDisplayedName(orgGroup.getDisplayedName().getMap())
			.withDescription(orgGroup.getDescription().getMap())
			.withEnableDelegation(orgGroup.getDelegationConfiguration().enabled)
			.withLogoUrl(orgGroup.getDelegationConfiguration().logoUrl)
			.withEnableSubprojects(orgGroup.getDelegationConfiguration().enableSubprojects)
			.withReadOnlyAttributes(orgGroup.getDelegationConfiguration().attributes)
			.withRegistrationForm(orgGroup.getDelegationConfiguration().registrationForm)
			.withSignUpEnquiry(orgGroup.getDelegationConfiguration().membershipUpdateEnquiryForm)
			.withMembershipUpdateEnquiry(orgGroup.getDelegationConfiguration().membershipUpdateEnquiryForm)
			.build();
	}

	private static RestProjectMembership map(GroupMembership membership)
	{
		return RestProjectMembership.builder()
			.withGroup(membership.getGroup())
			.withEntityId(membership.getEntityId())
			.withCreationTs(membership.getCreationTs().toInstant())
			.withTranslationProfile(membership.getTranslationProfile())
			.withRemoteIdp(membership.getRemoteIdp())
			.build();
	}
}
