/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

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

import javax.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

class RestProjectService
{
	private final DelegatedGroupManagement delGroupMan;
	private final GroupsManagement groupMan;
	private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private final UpmanRestAuthorizationManager authz;
	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;

	private final String rootGroup;
	private final String authorizationGroup;


	public RestProjectService(DelegatedGroupManagement delGroupMan,
	                          GroupsManagement groupMan,
	                          GroupDelegationConfigGenerator groupDelegationConfigGenerator,
	                          RegistrationsManagement registrationsManagement,
	                          EnquiryManagement enquiryManagement,
	                          UpmanRestAuthorizationManager authz,
	                          String rootGroup,
	                          String authorizationGroup)
	{
		this.delGroupMan = delGroupMan;
		this.groupMan = groupMan;
		this.groupDelegationConfigGenerator = groupDelegationConfigGenerator;
		this.registrationsManagement = registrationsManagement;
		this.enquiryManagement = enquiryManagement;
		this.authz = authz;
		this.rootGroup = rootGroup;
		this.authorizationGroup = authorizationGroup;
	}

	@Transactional
	public void addProject(RestProjectCreateRequest project) throws EngineException
	{
		assertAuthorization();

		String groupName;
		if(project.groupName == null)
		{
			GroupContents groupContent = groupMan.getContents(rootGroup, GroupContents.GROUPS);
			List<String> subGroups = groupContent.getSubGroups();
			groupName = "/" + generateName(subGroups);
		}
		else
			groupName = getFullGroupName(project.groupName);

		if (project.displayedName == null || project.displayedName.isEmpty())
			throw new IllegalArgumentException();

		Group toAdd = new Group(groupName);
		toAdd.setPublic(project.isPublic);
		toAdd.setDisplayedName(convertToI18nString(project.displayedName));
		toAdd.setDescription(convertToI18nString(project.description));

		groupMan.addGroup(toAdd);

		setDelegation(project.registrationForm, project.signUpEnquiry, project.membershipUpdateEnquiry,
			project.enableDelegation, project.registrationFormAutogenerate, groupName,
			project.logoUrl, project.signUpEnquiryAutogenerate, project.membershipUpdateEnquiryAutogenerate, toAdd,
			project.enableSubprojects, project.readOnlyAttributes);

		groupMan.updateGroup(groupName, toAdd);
	}

	@Transactional
	public void updateProject(String groupName, RestProjectUpdateRequest project) throws EngineException
	{
		assertAuthorization();

		if (project.displayedName == null || project.displayedName.isEmpty())
			throw new IllegalArgumentException();

		String fullGroupName = getFullGroupName(groupName);
		Group toUpdate = new Group(fullGroupName);
		toUpdate.setPublic(project.isPublic);
		toUpdate.setDisplayedName(convertToI18nString(project.displayedName));
		toUpdate.setDescription(convertToI18nString(project.description));

		setDelegation(project.registrationForm, project.signUpEnquiry, project.membershipUpdateEnquiry,
			project.enableDelegation, project.registrationFormAutogenerate, fullGroupName, project.logoUrl,
			project.signUpEnquiryAutogenerate, project.membershipUpdateEnquiryAutogenerate, toUpdate,
			project.enableSubprojects, project.readOnlyAttributes);

		groupMan.updateGroup(fullGroupName, toUpdate);
	}

	private void setDelegation(String registrationForm, String signUpEnquiry, String membershipUpdateEnquiry,
	                           boolean enableDelegation, boolean registrationFormAutogenerate,
	                           String fullGroupName, String logoUrl, boolean signUpEnquiryAutogenerate,
	                           boolean membershipUpdateEnquiryAutogenerate, Group toAdd, boolean enableSubprojects,
	                           List<String> readOnlyAttributes) throws EngineException
	{
		String registrationFormName = registrationForm;
		String joinEnquiryName = signUpEnquiry;
		String updateEnquiryName = membershipUpdateEnquiry;

		if (enableDelegation)
		{
			if (registrationFormAutogenerate)
			{
				RegistrationForm regForm = groupDelegationConfigGenerator
					.generateProjectRegistrationForm(
						fullGroupName, logoUrl, readOnlyAttributes);
				registrationsManagement.addForm(regForm);
				registrationFormName = regForm.getName();
			}

			if (signUpEnquiryAutogenerate)
			{
				EnquiryForm joinEnquiryForm = groupDelegationConfigGenerator
					.generateProjectJoinEnquiryForm(
						fullGroupName,
						logoUrl);
				enquiryManagement.addEnquiry(joinEnquiryForm);
				joinEnquiryName = joinEnquiryForm.getName();

			}
			if (membershipUpdateEnquiryAutogenerate)
			{
				EnquiryForm updateEnquiryForm = groupDelegationConfigGenerator
					.generateProjectUpdateEnquiryForm(
						fullGroupName,
						logoUrl);
				enquiryManagement.addEnquiry(updateEnquiryForm);
				updateEnquiryName = updateEnquiryForm.getName();
			}
		}

		toAdd.setDelegationConfiguration(new GroupDelegationConfiguration(enableDelegation,
			enableSubprojects,
			logoUrl,
			registrationFormName, joinEnquiryName, updateEnquiryName,
			readOnlyAttributes)
		);
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

	@Transactional
	public void removeProject(String groupName) throws EngineException
	{
		assertAuthorization();
		try
		{
			delGroupMan.removeGroup(rootGroup, getFullGroupName(groupName));
		} catch (Exception e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	@Transactional
	public RestProject getProject(String groupName) throws EngineException
	{
		assertAuthorization();
		GroupContents contents = groupMan.getContents(getFullGroupName(groupName),
			GroupContents.GROUPS | GroupContents.METADATA);
		Group group = contents.getGroup();
		return map(group);
	}

	@Transactional
	public List<RestProject> getProjects() throws EngineException
	{
		assertAuthorization();
		List<Group> groups = groupMan.getGroupsByWildcard(rootGroup + "/**");
		return groups.stream()
			.map(RestProjectService::map)
			.collect(Collectors.toList());
	}

	@Transactional
	public void addProjectMember(String groupName, long entityId) throws EngineException
	{
		assertAuthorization();
		delGroupMan.addMemberToGroup(rootGroup, getFullGroupName(groupName), entityId);
	}

	@Transactional
	public void removeProjectMember(String groupName, long entityId) throws EngineException
	{
		assertAuthorization();
		delGroupMan.removeMemberFromGroup(rootGroup, getFullGroupName(groupName), entityId);
	}

	@Transactional
	public List<RestProjectMembership> getProjectMembers(String groupName) throws EngineException
	{
		assertAuthorization();
		GroupContents contents = groupMan.getContents(getFullGroupName(groupName),
			GroupContents.MEMBERS);
		return contents.getMembers().stream()
			.map(RestProjectService::map)
			.collect(Collectors.toList());
	}

	@Transactional
	public RestProjectMembership getProjectMember(String groupName, long entityId) throws EngineException
	{
		assertAuthorization();
		GroupContents contents = groupMan.getContents(getFullGroupName(groupName),
			GroupContents.MEMBERS);
		return contents.getMembers().stream()
			.filter(member -> member.getEntityId() == entityId)
			.map(RestProjectService::map)
			.findFirst()
			.orElseThrow(IllegalArgumentException::new);
	}

	@Transactional
	public RestAuthorizationRole getProjectAuthorizationRole(String groupName, long entityId) throws EngineException
	{
		assertAuthorization();
		GroupAuthorizationRole groupAuthorizationRole = delGroupMan.getGroupAuthorizationRole(getFullGroupName(groupName), entityId);
		return new RestAuthorizationRole(groupAuthorizationRole.name());
	}

	@Transactional
	public void setProjectAuthorizationRole(String groupName, long entityId,
	                                                         RestAuthorizationRole role) throws EngineException
	{
		assertAuthorization();
		delGroupMan.setGroupAuthorizationRole(rootGroup, getFullGroupName(groupName), entityId, GroupAuthorizationRole.valueOf(role.role));
	}

	private String getFullGroupName(String groupName)
	{
		if(rootGroup.equals("/") && groupName.startsWith("/"))
			return groupName;
		else if (!rootGroup.equals("/") && !groupName.startsWith("/"))
			return rootGroup + "/" + groupName;
		else if (groupName.equals("/"))
			return rootGroup;
		return rootGroup + groupName;
	}

	private void assertAuthorization() throws AuthorizationException
	{
		authz.assertManagerAuthorization(authorizationGroup);
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
			.withSignUpEnquiry(orgGroup.getDelegationConfiguration().signupEnquiryForm)
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
