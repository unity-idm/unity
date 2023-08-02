/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import static java.util.Optional.ofNullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import io.imunity.upman.rest.DelegationComputer.RollbackState;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.entity.EntityWithContactInfo;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.engine.api.group.IllegalGroupValueException;
import pl.edu.icm.unity.engine.api.identity.UnknownEmailException;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;


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

		String projectPath = getProjectPath(projectId);
		if (project.displayedName == null || project.displayedName.isEmpty())
			throw new IllegalArgumentException();

		Group toAdd = new Group(projectPath);
		toAdd.setPublic(project.isPublic);
		toAdd.setDisplayedName(convertToI18nString(project.displayedName));
		toAdd.setDescription(convertToI18nString(project.description));

		groupMan.addGroup(toAdd);

		DelegationComputer delegationComputer = DelegationComputer.builder()
			.withFullGroupName(projectPath)
			.withLogoUrl(project.logoUrl)
			.withReadOnlyAttributes(project.readOnlyAttributes)
			.withGroupDelegationConfigGenerator(groupDelegationConfigGenerator)
			.withRegistrationsManagement(registrationsManagement)
			.withEnquiryManagement(enquiryManagement)
			.build();
		RollbackState rollbackState = delegationComputer.newRollbackState();
		try
		{
			String registrationFormName = delegationComputer.computeRegistrationFormName(project.registrationForm, rollbackState);
			String joinEnquiryName = delegationComputer.computeSignUpEnquiryName(project.signUpEnquiry, rollbackState);
			String updateEnquiryName = delegationComputer.computeMembershipUpdateEnquiryName(
					project.membershipUpdateEnquiry, rollbackState);
			toAdd.setDelegationConfiguration(new GroupDelegationConfiguration(true,
				project.enableSubprojects,
				project.logoUrl,
				registrationFormName, joinEnquiryName, updateEnquiryName,
				project.readOnlyAttributes)
			);

			groupMan.updateGroup(projectPath, toAdd);
		}
		catch (Exception e)
		{
			delegationComputer.rollback(rollbackState);
			groupMan.removeGroup(projectPath, false);
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

		String projectPath = getProjectPath(projectId);
		Group toUpdate = new Group(projectPath);
		toUpdate.setPublic(project.isPublic);
		toUpdate.setDisplayedName(convertToI18nString(project.displayedName));
		toUpdate.setDescription(convertToI18nString(project.description));

		DelegationComputer delegationComputer = DelegationComputer.builder()
			.withFullGroupName(projectPath)
			.withLogoUrl(project.logoUrl)
			.withReadOnlyAttributes(project.readOnlyAttributes)
			.withGroupDelegationConfigGenerator(groupDelegationConfigGenerator)
			.withRegistrationsManagement(registrationsManagement)
			.withEnquiryManagement(enquiryManagement)
			.build();
		RollbackState rollbackState = delegationComputer.newRollbackState();
		try
		{
			String registrationFormName = delegationComputer.computeRegistrationFormName(project.registrationForm, rollbackState);
			String joinEnquiryName = delegationComputer.computeSignUpEnquiryName(project.signUpEnquiry, rollbackState);
			String updateEnquiryName = delegationComputer.computeMembershipUpdateEnquiryName(project.membershipUpdateEnquiry, rollbackState);
			toUpdate.setDelegationConfiguration(new GroupDelegationConfiguration(true,
				project.enableSubprojects,
				project.logoUrl,
				registrationFormName, joinEnquiryName, updateEnquiryName,
				project.readOnlyAttributes)
			);
			groupMan.updateGroup(projectPath, toUpdate);
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
			groupMan.removeGroup(getProjectPath(projectId), true);
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
			contents = groupMan.getContents(getProjectPath(projectId),
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
		String projectPath = getProjectPath(projectId);
		delGroupMan.addMemberToGroup(projectPath, projectPath, id);
	}

	private void validateGroupPresence(String projectId) throws AuthorizationException
	{
		if (!groupMan.isPresent(getProjectPath(projectId)))
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
			groupMan.removeMember(getProjectPath(projectId), new EntityParam(new IdentityTaV(EmailIdentity.ID, email)));
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
		String projectPath = getProjectPath(projectId);
		return delGroupMan.getDelegatedGroupMembers(projectPath, projectPath).stream()
			.map(RestProjectService::map)
			.collect(Collectors.toList());
	}

	@Transactional
	public RestProjectMembership getProjectMember(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		validateGroupPresence(projectId);
		String projectPath = getProjectPath(projectId);
		return delGroupMan.getDelegatedGroupMembers(projectPath, projectPath).stream()
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
			String projectPath = getProjectPath(projectId);
			delGroupMan.setGroupAuthorizationRole(projectPath, projectPath, id,
				GroupAuthorizationRole.valueOf(role.role));
		}
		catch (IllegalGroupValueException e)
		{
			throw new NotFoundException("Entity is not a member of the project");
		}
	}

	private Long getId(String email) throws EngineException
	{
		Set<EntityWithContactInfo> entities;
		try
		{
			entities = idsMan.getAllEntitiesWithContactEmails(Set.of(email));
		}
		catch (UnknownEmailException e)
		{
			throw new NotFoundException(String.format("Email %s not found", email));
		}

		if(entities.size() > 1)
			throw new BadRequestException("Ambiguous user");
		if(entities.size() == 0)
			throw new NotFoundException(String.format("Email %s not found", email));
		return entities.iterator().next().entity.getId();
	}

	private String getProjectPath(String projectId)
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
