/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.IdentityTaV;

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

		new DelegationSetter(project.registrationForm, project.signUpEnquiry, project.membershipUpdateEnquiry,
			fullGroupName, project.logoUrl, project.enableSubprojects, project.readOnlyAttributes, groupDelegationConfigGenerator,
			registrationsManagement, enquiryManagement).setFor(toAdd);

		groupMan.updateGroup(fullGroupName, toAdd);
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

		new DelegationSetter(project.registrationForm, project.signUpEnquiry, project.membershipUpdateEnquiry,
			fullGroupName, project.logoUrl, project.enableSubprojects, project.readOnlyAttributes, groupDelegationConfigGenerator,
			registrationsManagement, enquiryManagement).setFor(toUpdate);

		groupMan.updateGroup(fullGroupName, toUpdate);
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
	public void removeProject(String projectId) throws EngineException
	{
		assertAuthorization();
		try
		{
			groupMan.removeGroup(getFullGroupName(projectId), true);
		} catch (Exception e)
		{
			throw new IllegalArgumentException(e);
		}
	}

	@Transactional
	public RestProject getProject(String projectId) throws EngineException
	{
		assertAuthorization();
		GroupContents contents = groupMan.getContents(getFullGroupName(projectId),
			GroupContents.GROUPS | GroupContents.METADATA);
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
			.map(group -> map(group.getName().replace(rootGroup + "/", ""), group))
			.collect(Collectors.toList());
	}

	@Transactional
	public void addProjectMember(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		groupMan.addMemberFromParent(getFullGroupName(projectId), new EntityParam(new IdentityTaV(EmailIdentity.ID, email)));
	}

	@Transactional
	public void removeProjectMember(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		groupMan.removeMember(getFullGroupName(projectId), new EntityParam(new IdentityTaV(EmailIdentity.ID, email)));
	}

	@Transactional
	public List<RestProjectMembership> getProjectMembers(String projectId) throws EngineException
	{
		assertAuthorization();
		return delGroupMan.getDelegatedGroupMemebers(rootGroup, getFullGroupName(projectId)).stream()
			.map(RestProjectService::map)
			.collect(Collectors.toList());
	}

	@Transactional
	public RestProjectMembership getProjectMember(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		return delGroupMan.getDelegatedGroupMemebers(rootGroup, getFullGroupName(projectId)).stream()
			.filter(member -> member.email.getValue().equals(email))
			.map(RestProjectService::map)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("There is no membership"));
	}

	@Transactional
	public RestAuthorizationRole getProjectAuthorizationRole(String projectId, String email) throws EngineException
	{
		assertAuthorization();
		Long id = idsMan.getEntity(new EntityParam(new IdentityTaV(EmailIdentity.ID, email))).getId();
		GroupAuthorizationRole groupAuthorizationRole =
			delGroupMan.getGroupAuthorizationRole(getFullGroupName(projectId), id);
		return new RestAuthorizationRole(groupAuthorizationRole.name());
	}

	@Transactional
	public void setProjectAuthorizationRole(String projectId, String email,
	                                                         RestAuthorizationRole role) throws EngineException
	{
		assertAuthorization();
		Long id = idsMan.getEntity(new EntityParam(new IdentityTaV(EmailIdentity.ID, email))).getId();
		delGroupMan.setGroupAuthorizationRole(rootGroup, getFullGroupName(projectId), id, GroupAuthorizationRole.valueOf(role.role));
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
