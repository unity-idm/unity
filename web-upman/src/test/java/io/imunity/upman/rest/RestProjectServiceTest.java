/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.entity.EntityWithContactInfo;
import pl.edu.icm.unity.engine.api.group.GroupNotFoundException;
import pl.edu.icm.unity.engine.api.identity.UnknownEmailException;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.IdentityTaV;
import pl.edu.icm.unity.types.basic.VerifiableElementBase;

@ExtendWith(MockitoExtension.class)
class RestProjectServiceTest
{
	@Mock
	private DelegatedGroupManagement delGroupMan;
	@Mock
	private GroupsManagement groupMan;
	@Mock
	private UpmanRestAuthorizationManager authz;
	@Mock
	private EntityManagement idsMan;
	@Mock
	private ProjectGroupProvider projectGroupProvider;
	@Mock
	private RegistrationsManagement registrationsManagement;
	@Mock
	private EnquiryManagement enquiryManagement;
	
	private RestProjectService restProjectService;
	

	@BeforeEach
	void setUp()
	{
		restProjectService = new RestProjectService(delGroupMan, groupMan, authz, idsMan,
				registrationsManagement, enquiryManagement, "/A", "A/B");
	}

	@Test
	void shouldAddProjectWithNullForms() throws EngineException
	{
		RestProjectCreateRequest request = RestProjectCreateRequest.builder()
			.withProjectId("B")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "CoolGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("logoUrl")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.build();

		restProjectService.addProject(request);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(groupMan).addGroup(argument.capture());
		assertThat(argument.getValue().getPathEncoded()).isEqualTo("/A/B");
		assertThat(argument.getValue().getDelegationConfiguration().registrationForm).isEqualTo(null);
		assertThat(argument.getValue().getDelegationConfiguration().signupEnquiryForm).isEqualTo(null);
		assertThat(argument.getValue().getDelegationConfiguration().membershipUpdateEnquiryForm).isEqualTo(null);
	}

	@Test
	void shouldUpdateProjectWithNullForms() throws EngineException
	{
		RestProjectUpdateRequest request = RestProjectUpdateRequest.builder()
			.withPublic(false)
			.withDisplayedName(Map.of("en", "CoolGroup"))
			.withDescription(Map.of("en", "description"))
			.withEnableDelegation(true)
			.withLogoUrl("logoUrl")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.build();

		setUpGroupContent(new GroupDelegationConfiguration(true));
		
		restProjectService.updateProject("B", request);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(groupMan).updateGroup(eq("/A/B"), argument.capture());
		assertThat(argument.getValue().getDelegationConfiguration().registrationForm).isEqualTo(null);
		assertThat(argument.getValue().getDelegationConfiguration().signupEnquiryForm).isEqualTo(null);
		assertThat(argument.getValue().getDelegationConfiguration().membershipUpdateEnquiryForm).isEqualTo(null);
	}

	@Test
	void shouldRemoveProject() throws EngineException
	{
		setUpGroupContent(new GroupDelegationConfiguration(true));

		restProjectService.removeProject("B");

		verify(groupMan).removeGroup("/A/B", true);
	}
	
	@Test
	void shouldRemoveProjectWithForms() throws EngineException
	{
		Group group = setUpGroupContent(GroupDelegationConfiguration.builder()
				.withRegistrationForm("regForm")
				.withMembershipUpdateEnquiryForm("updateEnquiry")
				.withSignupEnquiryForm("signupEnquiry")
				.build());

		restProjectService.removeProject("B");
		group.setDelegationConfiguration(new GroupDelegationConfiguration(true));
		verify(groupMan).updateGroup("/A/B", group);
		verify(groupMan).removeGroup("/A/B", true);
		verify(registrationsManagement).removeForm("regForm", true);
		verify(enquiryManagement).removeEnquiry("updateEnquiry", true);
		verify(enquiryManagement).removeEnquiry("signupEnquiry", true);
	}

	@Test
	void shouldGetProject() throws EngineException
	{
		setUpGroupContent(new GroupDelegationConfiguration(
				true, true, "logoUrl", "regForm", "sigForm", "memForm", List.of("attr"), List.of()
				));
		RestProject project = restProjectService.getProject("B");
		assertThat(project.projectId).isEqualTo("B");
		assertThat(project.isPublic).isEqualTo(true);
		assertThat(project.displayedName).isEqualTo(Map.of("en", "disName"));
		assertThat(project.description).isEqualTo(Map.of("en", "description"));
		assertThat(project.logoUrl).isEqualTo("logoUrl");
		assertThat(project.enableSubprojects).isEqualTo(true);
		assertThat(project.readOnlyAttributes).isEqualTo(List.of("attr"));
		assertThat(project.registrationForm).isEqualTo("regForm");
		assertThat(project.signUpEnquiry).isEqualTo("sigForm");
		assertThat(project.membershipUpdateEnquiry).isEqualTo("memForm");
	}

	@Test
	void shouldGetProjects() throws EngineException
	{
		Group group = new Group("/A/B");
		group.setPublic(true);
		group.setDisplayedName(convertToI18nString(Map.of("en", "disName")));
		group.setDescription(convertToI18nString(Map.of("en", "description")));
		group.setDelegationConfiguration(new GroupDelegationConfiguration(
			true, true, "logoUrl", "regForm", "sigForm", "memForm", List.of("attr"), List.of()
		));
		when(groupMan.getGroupsByWildcard("/A/**"))
			.thenReturn(List.of(group));

		List<RestProject> projects = restProjectService.getProjects();
		assertThat(projects.size()).isEqualTo(1);
		RestProject project = projects.iterator().next();
		assertThat(project.projectId).isEqualTo("B");
		assertThat(project.isPublic).isEqualTo(true);
		assertThat(project.displayedName).isEqualTo(Map.of("en", "disName"));
		assertThat(project.description).isEqualTo(Map.of("en", "description"));
		assertThat(project.logoUrl).isEqualTo("logoUrl");
		assertThat(project.enableSubprojects).isEqualTo(true);
		assertThat(project.readOnlyAttributes).isEqualTo(List.of("attr"));
		assertThat(project.registrationForm).isEqualTo("regForm");
		assertThat(project.signUpEnquiry).isEqualTo("sigForm");
		assertThat(project.membershipUpdateEnquiry).isEqualTo("memForm");
	}

	@Test
	void shouldAddProjectMember() throws EngineException
	{
		long id = 2L;
		Entity entity = mock(Entity.class);
		when(idsMan.getAllEntitiesWithContactEmails(Set.of("email")))
		.thenReturn(Set.of(new EntityWithContactInfo(entity, "email", Set.of("/"))));
		when(entity.getId()).thenReturn(id);
		setUpGroupContent(new GroupDelegationConfiguration(true));
		restProjectService.addProjectMember("B", "email");

		verify(delGroupMan).addMemberToGroup("/A/B", "/A/B", id);
	}

	@Test
	void shouldNotAddProjectMemberWhenGroupDoesntExist() throws EngineException
	{
		when(groupMan.getContents("/A/B", 8)).thenThrow(GroupNotFoundException.class);
		Assertions.assertThrows(NotFoundException.class, () -> restProjectService.addProjectMember("B", "email"));
	}

	@Test
	void shouldNotAddProjectMemberWhenUserIdentityDoesntExist() throws EngineException
	{
		when(idsMan.getAllEntitiesWithContactEmails(Set.of("email")))
			.thenThrow(UnknownEmailException.class);
		setUpGroupContent(new GroupDelegationConfiguration(true));

		Assertions.assertThrows(NotFoundException.class, () -> restProjectService.addProjectMember("B", "email"));
	}

	@Test
	void shouldRemoveProjectMember() throws EngineException
	{
		setUpGroupContent(new GroupDelegationConfiguration(true));

		restProjectService.removeProjectMember("B", "email");

		verify(groupMan).removeMember("/A/B", new EntityParam(new IdentityTaV(EmailIdentity.ID, "email")));
	}

	@Test
	void shouldGetProjectMember() throws EngineException
	{
		

		when(delGroupMan.getDelegatedGroupMembers("/A/B", "/A/B"))
			.thenReturn(List.of(
				new DelegatedGroupMember(2, "/A/B", "/B", GroupAuthorizationRole.manager,
				"name", new VerifiableElementBase("email@gmail.com"),
					Optional.of(List.of(new Attribute("attr", "string", "/A/B", List.of("val"))))))
			);
		setUpGroupContent(new GroupDelegationConfiguration(true));
		
		RestProjectMembership membership = restProjectService.getProjectMember("B", "email@gmail.com");

		assertThat(membership.email).isEqualTo("email@gmail.com");
		assertThat(membership.role).isEqualTo(GroupAuthorizationRole.manager.name());
		assertThat(membership.attributes).isEqualTo(List.of(new RestAttribute("attr", List.of("val"))));
	}

	@Test
	void shouldGetProjectMembers() throws EngineException
	{
		GroupContents groupContents = new GroupContents();
		groupContents.setMembers(List.of(new GroupMembership("/A/B", 2, new Date())));

		when(delGroupMan.getDelegatedGroupMembers("/A/B", "/A/B"))
			.thenReturn(List.of(
				new DelegatedGroupMember(2, "/A/B", "/B", GroupAuthorizationRole.manager,
					"name", new VerifiableElementBase("email@gmail.com"),
					Optional.of(List.of(new Attribute("attr", "string", "/A/B", List.of("val"))))))
			);
		setUpGroupContent(new GroupDelegationConfiguration(true));

		List<RestProjectMembership> members = restProjectService.getProjectMembers("B");

		assertThat(members.size()).isEqualTo(1);
		RestProjectMembership membership = members.iterator().next();
		assertThat(membership.email).isEqualTo("email@gmail.com");
		assertThat(membership.role).isEqualTo(GroupAuthorizationRole.manager.name());
		assertThat(membership.attributes).isEqualTo(List.of(new RestAttribute("attr", List.of("val"))));
	}

	@Test
	void shouldGetProjectAuthorizationRole() throws EngineException
	{
		when(delGroupMan.getDelegatedGroupMembers("/A/B", "/A/B"))
			.thenReturn(List.of(
				new DelegatedGroupMember(2, "/A/B", "/B", GroupAuthorizationRole.manager,
					"name", new VerifiableElementBase("email@gmail.com"),
					Optional.of(List.of(new Attribute("attr", "string", "/A/B", List.of("val"))))))
			);
		setUpGroupContent(new GroupDelegationConfiguration(true));

		RestAuthorizationRole role = restProjectService.getProjectAuthorizationRole("B", "email@gmail.com");

		assertThat(role).isEqualTo(new RestAuthorizationRole("manager"));
	}

	@Test
	void shouldSetProjectAuthorizationRole() throws EngineException
	{
		long id = 2L;
		Entity entity = mock(Entity.class);
		when(idsMan.getAllEntitiesWithContactEmails(Set.of("email")))
			.thenReturn(Set.of(new EntityWithContactInfo(entity, "email", Set.of("/"))));
		when(entity.getId()).thenReturn(id);
		setUpGroupContent(new GroupDelegationConfiguration(true));


		restProjectService.setProjectAuthorizationRole("B", "email", new RestAuthorizationRole("manager"));

		verify(delGroupMan).setGroupAuthorizationRole("/A/B", "/A/B", 2, GroupAuthorizationRole.manager);
	}
	
	
	
	private I18nString convertToI18nString(Map<String, String> map)
	{
		I18nString i18nString = new I18nString();
		i18nString.addAllValues(map);
		Optional.ofNullable(map.get("")).ifPresent(i18nString::setDefaultValue);
		return i18nString;
	}
	
	private Group setUpGroupContent(GroupDelegationConfiguration groupDelegationConfiguration) throws EngineException
	{
		GroupContents content = new GroupContents();
		Group group = new Group("/A/B");
		group.setPublic(true);
		group.setDisplayedName(convertToI18nString(Map.of("en", "disName")));
		group.setDescription(convertToI18nString(Map.of("en", "description")));
		group.setDelegationConfiguration(groupDelegationConfiguration);
		content.setGroup(group);
		when(groupMan.getContents("/A/B", GroupContents.METADATA)).thenReturn(content);
		return group;
	}
}
