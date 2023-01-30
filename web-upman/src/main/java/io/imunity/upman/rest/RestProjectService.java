/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;
import pl.edu.icm.unity.exceptions.UnknownIdentityException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.IdentityTaV;

import javax.transaction.Transactional;
import javax.ws.rs.NotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

class RestProjectService
{
	private final DelegatedGroupManagement delGroupMan;
	private final GroupsManagement groupMan;
	private final GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	private final UpmanRestAuthorizationManager authz;
	private final RegistrationsManagement registrationsManagement;
	private final EnquiryManagement enquiryManagement;
	private final EntityManagement idsMan;

	private final String rootGroup;
	private final String authorizationGroup;


	public RestProjectService(DelegatedGroupManagement delGroupMan,
	                          GroupsManagement groupMan,
	                          GroupDelegationConfigGenerator groupDelegationConfigGenerator,
	                          RegistrationsManagement registrationsManagement,
	                          EnquiryManagement enquiryManagement,
	                          UpmanRestAuthorizationManager authz,
	                          EntityManagement idsMan,
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
		this.idsMan = idsMan;
	}

	@Transactional
	public RestProjectId addProject(RestProjectCreateRequest project) throws EngineException
	{
		assertAuthorization();

		String projectId;
		if(project.projectId == null)
		{
			GroupContents groupContent = groupMan.getContents(rootGroup, GroupContents.GROUPS);
			List<String> subGroups = groupContent.getSubGroups();
			projectId = generateName(subGroups);
		}
		else
			projectId = project.projectId;

		String fullGroupName = getFullGroupName(projectId);
		if (project.displayedName == null || project.displayedName.isEmpty())
			throw new IllegalArgumentException();

		Group toAdd = new Group(fullGroupName);
		toAdd.setPublic(project.isPublic);
		toAdd.setDisplayedName(convertToI18nString(project.displayedName));
		toAdd.setDescription(convertToI18nString(project.description));

		groupMan.addGroup(toAdd);

		DelegationComputer delegationComputer = DelegationComputer.builder()
			.withFullGroupName(fullGroupName)
			.withLogoUrl(project.logoUrl)
			.withReadOnlyAttributes(project.readOnlyAttributes)
			.withGroupDelegationConfigGenerator(groupDelegationConfigGenerator)
			.withRegistrationsManagement(registrationsManagement)
			.withEnquiryManagement(enquiryManagement)
			.build();

		try
		{
			String registrationFormName = delegationComputer.computeRegistrationFormName(project.registrationForm);
			String joinEnquiryName = delegationComputer.computeSignUpEnquiryName(project.signUpEnquiry);
			String updateEnquiryName = delegationComputer.computeMembershipUpdateEnquiryName(project.membershipUpdateEnquiry);
			toAdd.setDelegationConfiguration(new GroupDelegationConfiguration(true,
				project.enableSubprojects,
				project.logoUrl,
				registrationFormName, joinEnquiryName, updateEnquiryName,
				project.readOnlyAttributes)
			);

			groupMan.updateGroup(fullGroupName, toAdd);
		}
		catch (Exception e)
		{
			groupMan.removeGroup(fullGroupName, false);
			throw e;
		}

		return new RestProjectId(projectId);
	}

	@Transactional
	public void updateProject(String projectId, RestProjectUpdateRequest project) throws EngineException
	{
		assertAuthorization();

		if (project.displayedName == null || project.displayedName.isEmpty())
			throw new IllegalArgumentException("Displayed name have to be set");

		String fullGroupName = getFullGroupName(projectId);
		Group toUpdate = new Group(fullGroupName);
		toUpdate.setPublic(project.isPublic);
		toUpdate.setDisplayedName(convertToI18nString(project.displayedName));
		toUpdate.setDescription(convertToI18nString(project.description));

		DelegationComputer delegationComputer = DelegationComputer.builder()
			.withFullGroupName(fullGroupName)
			.withLogoUrl(project.logoUrl)
			.withReadOnlyAttributes(project.readOnlyAttributes)
			.withGroupDelegationConfigGenerator(groupDelegationConfigGenerator)
			.withRegistrationsManagement(registrationsManagement)
			.withEnquiryManagement(enquiryManagement)
			.build();
		try
		{
			String registrationFormName = delegationComputer.computeRegistrationFormName(project.registrationForm);
			String joinEnquiryName = delegationComputer.computeSignUpEnquiryName(project.signUpEnquiry);
			String updateEnquiryName = delegationComputer.computeMembershipUpdateEnquiryName(project.membershipUpdateEnquiry);
			toUpdate.setDelegationConfiguration(new GroupDelegationConfiguration(true,
				project.enableSubprojects,
				project.logoUrl,
				registrationFormName, joinEnquiryName, updateEnquiryName,
				project.readOnlyAttributes)
			);
			groupMan.updateGroup(fullGroupName, toUpdate);
		}
		catch (GroupNotFoundException e)
		{
			throw new NotFoundException(e);
		}
	}

	private I18nString convertToI18nString(Map<String, String> map)
	{
		I18nString i18nString = new I18nString();
		i18nString.addAllValues(map);
		ofNullable(map.get("")).ifPresent(i18nString::setDefaultValue);
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
	public void removeProject(String projectId) throws EngineException
	{
		assertAuthorization();
		try
		{
			groupMan.removeGroup(getFullGroupName(projectId), true);
		}
		catch (GroupNotFoundException e)
		{
			throw new NotFoundException(e);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	@Transactional
	public RestProject getProject(String projectId) throws EngineException
	{
		assertAuthorization();
		GroupContents contents;
		try
		{
			contents = groupMan.getContents(getFullGroupName(projectId),
				GroupContents.GROUPS | GroupContents.METADATA);
		}
		catch (GroupNotFoundException e)
		{
			throw new NotFoundException(e);
		}

		Group group = contents.getGroup();
		return map(projectId, group);
	}

	@Transactional
	public List<RestProject> getProjects() throws EngineException
	{
		assertAuthorization();
		List<Group> groups = groupMan.getGroupsByWildcard(rootGroup + "/**");
		return groups.stream()
			.filter(group -> !group.getName().equals(rootGroup))
			.filter(group -> group.getDelegationConfiguration().enabled)
			.map(group -> map(group.getName().replace(rootGroup + "/", ""), group))
			.collect(Collectors.toList());
	}

	@Transactional
	public void addProjectMember(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		Long id = getId(email);
		delGroupMan.addMemberToGroup(rootGroup, getFullGroupName(projectId), id);
	}

	private void validateGroupPresence(String projectId) throws AuthorizationException
	{
		if (!groupMan.isPresent(getFullGroupName(projectId)))
			throw new NotFoundException(String.format("Project %s doesn't exist", projectId));
	}

	private void validateRole(RestAuthorizationRole role)
	{
		if (Arrays.stream(GroupAuthorizationRole.values()).noneMatch(authzRole -> authzRole.name().equals(role.role)))
			throw new IllegalArgumentException(String.format("Invalid role: %s, allowed values: manager, " +
				"projectsAdmin, regular", role.role));
	}

	@Transactional
	public void removeProjectMember(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		try
		{
			groupMan.removeMember(getFullGroupName(projectId), new EntityParam(new IdentityTaV(EmailIdentity.ID, email)));
		}
		catch (UnknownIdentityException e)
		{
			throw new NotFoundException(String.format("Email %s not found", email));
		}
	}

	@Transactional
	public List<RestProjectMembership> getProjectMembers(String projectId) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		String fullGroupName = getFullGroupName(projectId);
		return delGroupMan.getDelegatedGroupMemebers(fullGroupName, getFullGroupName(projectId)).stream()
			.map(RestProjectService::map)
			.collect(Collectors.toList());
	}

	@Transactional
	public RestProjectMembership getProjectMember(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		String fullGroupName = getFullGroupName(projectId);
		return delGroupMan.getDelegatedGroupMemebers(fullGroupName, fullGroupName).stream()
			.filter(member -> member.email.getValue().equals(email))
			.map(RestProjectService::map)
			.findFirst()
			.orElseThrow(() -> new NotFoundException("There is no member"));
	}

	@Transactional
	public RestAuthorizationRole getProjectAuthorizationRole(String projectId, String email) throws EngineException
	{
		return new RestAuthorizationRole(getProjectMember(projectId, email).role);
	}

	@Transactional
	public void setProjectAuthorizationRole(String projectId, String email,
	                                                         RestAuthorizationRole role) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		validateRole(role);
		Long id = getId(email);
		try
		{
			delGroupMan.setGroupAuthorizationRole(rootGroup, getFullGroupName(projectId), id, GroupAuthorizationRole.valueOf(role.role));
		}
		catch (IllegalGroupValueException e)
		{
			throw new NotFoundException("Entity is not a member of the project");
		}
	}

	private Long getId(String email) throws EngineException
	{
		try
		{
			return idsMan.getEntity(new EntityParam(new IdentityTaV(EmailIdentity.ID, email))).getId();
		}
		catch (UnknownIdentityException e)
		{
			throw new NotFoundException(String.format("Email %s not found", email));
		}
	}

	private String getFullGroupName(String projectId)
	{
		if(projectId.contains("/"))
			throw new IllegalArgumentException("Project Id cannot start form /");
		return rootGroup + "/" + projectId;
	}

	private void assertAuthorization() throws AuthorizationException
	{
		authz.assertManagerAuthorization(authorizationGroup);
	}

	private static RestProject map(String projectId, Group group)
	{
		return RestProject.builder()
			.withProjectId(projectId)
			.withPublic(group.isPublic())
			.withDisplayedName(group.getDisplayedName().getMap())
			.withDescription(group.getDescription().getMap())
			.withEnableDelegation(group.getDelegationConfiguration().enabled)
			.withLogoUrl(group.getDelegationConfiguration().logoUrl)
			.withEnableSubprojects(group.getDelegationConfiguration().enableSubprojects)
			.withReadOnlyAttributes(group.getDelegationConfiguration().attributes)
			.withRegistrationForm(group.getDelegationConfiguration().registrationForm)
			.withSignUpEnquiry(group.getDelegationConfiguration().signupEnquiryForm)
			.withMembershipUpdateEnquiry(group.getDelegationConfiguration().membershipUpdateEnquiryForm)
			.build();
	}

	private static RestProjectMembership map(DelegatedGroupMember member)
	{
		return RestProjectMembership.builder()
			.withEmail(member.email.getValue())
			.withRole(member.role.name())
			.withAttributes(
				member.attributes.stream()
					.map(attr -> new RestAttribute(attr.getName(), List.copyOf(attr.getValues())))
					.collect(Collectors.toList())
			)
			.build();
	}
}
