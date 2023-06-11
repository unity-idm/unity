/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.rest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.entity.IdentityTaV;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.verifiable.VerifiableElementBase;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.identity.UnknownEmailException;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.utils.GroupDelegationConfigGenerator;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;

import javax.ws.rs.NotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestProjectServiceTest
{
	@Mock
	private DelegatedGroupManagement delGroupMan;
	@Mock
	private GroupsManagement groupMan;
	@Mock
	private GroupDelegationConfigGenerator groupDelegationConfigGenerator;
	@Mock
	private UpmanRestAuthorizationManager authz;
	@Mock
	private RegistrationsManagement registrationsManagement;
	@Mock
	private EnquiryManagement enquiryManagement;
	@Mock
	private EntityManagement idsMan;
	private RestProjectService restProjectService;

	@BeforeEach
	void setUp()
	{
		restProjectService = new RestProjectService(
			delGroupMan, groupMan, groupDelegationConfigGenerator,
			registrationsManagement, enquiryManagement, authz, idsMan, "/A", "/A/B"
		);
	}

	@Test
	void shouldAddProjectWithAutogenerateForms() throws EngineException
	{
		RestProjectCreateRequest request = RestProjectCreateRequest.builder()
			.withProjectId("B")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "CoolGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("logoUrl")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();

		RegistrationForm registrationForm = mock(RegistrationForm.class);
		when(groupDelegationConfigGenerator.generateProjectRegistrationForm("/A/B", "logoUrl", List.of()))
			.thenReturn(registrationForm);
		when(registrationForm.getName()).thenReturn("regName");

		EnquiryForm enquiryForm = mock(EnquiryForm.class);
		when(groupDelegationConfigGenerator.generateProjectJoinEnquiryForm("/A/B", "logoUrl"))
			.thenReturn(enquiryForm);
		when(enquiryForm.getName()).thenReturn("enqName");

		EnquiryForm updateEnquiryForm = mock(EnquiryForm.class);
		when(groupDelegationConfigGenerator.generateProjectUpdateEnquiryForm("/A/B", "logoUrl"))
			.thenReturn(updateEnquiryForm);
		when(updateEnquiryForm.getName()).thenReturn("updateEnqName");

		restProjectService.addProject(request);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(groupMan).addGroup(argument.capture());
		assertThat(argument.getValue().getPathEncoded()).isEqualTo("/A/B");
		assertThat(argument.getValue().getDelegationConfiguration().registrationForm).isEqualTo("regName");
		assertThat(argument.getValue().getDelegationConfiguration().signupEnquiryForm).isEqualTo("enqName");
		assertThat(argument.getValue().getDelegationConfiguration().membershipUpdateEnquiryForm).isEqualTo("updateEnqName");
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
			.withRegistrationForm(new RestRegistrationForm(null, false))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, false))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, false))
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
	void shouldAddProjectWithDefinedForms() throws EngineException
	{
		RestProjectCreateRequest request = RestProjectCreateRequest.builder()
			.withProjectId("B")
			.withPublic(false)
			.withDisplayedName(Map.of("en", "CoolGroup"))
			.withDescription(Map.of("en", "description"))
			.withLogoUrl("logoUrl")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm("ala", false))
			.withSignUpEnquiry(new RestSignUpEnquiry("ola", false))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry("tola", false))
			.build();
		when(registrationsManagement.hasForm("ala")).thenReturn(true);
		when(enquiryManagement.hasForm("ola")).thenReturn(true);
		when(enquiryManagement.hasForm("tola")).thenReturn(true);

		restProjectService.addProject(request);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(groupMan).addGroup(argument.capture());
		assertThat(argument.getValue().getPathEncoded()).isEqualTo("/A/B");
		assertThat(argument.getValue().getDelegationConfiguration().registrationForm).isEqualTo("ala");
		assertThat(argument.getValue().getDelegationConfiguration().signupEnquiryForm).isEqualTo("ola");
		assertThat(argument.getValue().getDelegationConfiguration().membershipUpdateEnquiryForm).isEqualTo("tola");
	}

	@Test
	void shouldUpdateProjectWithAutogenerateForms() throws EngineException
	{
		RestProjectUpdateRequest request = RestProjectUpdateRequest.builder()
			.withPublic(false)
			.withDisplayedName(Map.of("en", "CoolGroup"))
			.withDescription(Map.of("en", "description"))
			.withEnableDelegation(true)
			.withLogoUrl("logoUrl")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm(null, true))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, true))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, true))
			.build();

		RegistrationForm registrationForm = mock(RegistrationForm.class);
		when(groupDelegationConfigGenerator.generateProjectRegistrationForm("/A/B", "logoUrl", List.of()))
			.thenReturn(registrationForm);
		when(registrationForm.getName()).thenReturn("regName");

		EnquiryForm enquiryForm = mock(EnquiryForm.class);
		when(groupDelegationConfigGenerator.generateProjectJoinEnquiryForm("/A/B", "logoUrl"))
			.thenReturn(enquiryForm);
		when(enquiryForm.getName()).thenReturn("enqName");

		EnquiryForm updateEnquiryForm = mock(EnquiryForm.class);
		when(groupDelegationConfigGenerator.generateProjectUpdateEnquiryForm("/A/B", "logoUrl"))
			.thenReturn(updateEnquiryForm);
		when(updateEnquiryForm.getName()).thenReturn("updateEnqName");

		restProjectService.updateProject("B", request);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(groupMan).updateGroup(eq("/A/B"), argument.capture());
		assertThat(argument.getValue().getDelegationConfiguration().registrationForm).isEqualTo("regName");
		assertThat(argument.getValue().getDelegationConfiguration().signupEnquiryForm).isEqualTo("enqName");
		assertThat(argument.getValue().getDelegationConfiguration().membershipUpdateEnquiryForm).isEqualTo("updateEnqName");
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
			.withRegistrationForm(new RestRegistrationForm(null, false))
			.withSignUpEnquiry(new RestSignUpEnquiry(null, false))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry(null, false))
			.build();

		restProjectService.updateProject("B", request);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(groupMan).updateGroup(eq("/A/B"), argument.capture());
		assertThat(argument.getValue().getDelegationConfiguration().registrationForm).isEqualTo(null);
		assertThat(argument.getValue().getDelegationConfiguration().signupEnquiryForm).isEqualTo(null);
		assertThat(argument.getValue().getDelegationConfiguration().membershipUpdateEnquiryForm).isEqualTo(null);
	}

	@Test
	void shouldUpdateProjectWithDefinedForms() throws EngineException
	{
		RestProjectUpdateRequest request = RestProjectUpdateRequest.builder()
			.withPublic(false)
			.withDisplayedName(Map.of("en", "CoolGroup"))
			.withDescription(Map.of("en", "description"))
			.withEnableDelegation(true)
			.withLogoUrl("logoUrl")
			.withEnableSubprojects(true)
			.withReadOnlyAttributes(List.of())
			.withRegistrationForm(new RestRegistrationForm("ala", false))
			.withSignUpEnquiry(new RestSignUpEnquiry("ola", false))
			.withMembershipUpdateEnquiry(new RestMembershipEnquiry("tola", false))
			.build();

		when(registrationsManagement.hasForm("ala")).thenReturn(true);
		when(enquiryManagement.hasForm("ola")).thenReturn(true);
		when(enquiryManagement.hasForm("tola")).thenReturn(true);

		restProjectService.updateProject("B", request);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(groupMan).updateGroup(eq("/A/B"), argument.capture());
		assertThat(argument.getValue().getDelegationConfiguration().registrationForm).isEqualTo("ala");
		assertThat(argument.getValue().getDelegationConfiguration().signupEnquiryForm).isEqualTo("ola");
		assertThat(argument.getValue().getDelegationConfiguration().membershipUpdateEnquiryForm).isEqualTo("tola");
	}

	@Test
	void shouldRemoveProject() throws EngineException
	{
		restProjectService.removeProject("B");

		verify(groupMan).removeGroup("/A/B", true);
	}

	@Test
	void shouldGetProject() throws EngineException
	{
		GroupContents groupContents = new GroupContents();
		Group group = new Group("/A/B");
		group.setPublic(true);
		group.setDisplayedName(convertToI18nString(Map.of("en", "disName")));
		group.setDescription(convertToI18nString(Map.of("en", "description")));
		group.setDelegationConfiguration(new GroupDelegationConfiguration(
			true, true, "logoUrl", "regForm", "sigForm", "memForm", List.of("attr")
		));
		groupContents.setGroup(group);

		when(groupMan.getContents("/A/B", GroupContents.GROUPS | GroupContents.METADATA))
			.thenReturn(groupContents);

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
			true, true, "logoUrl", "regForm", "sigForm", "memForm", List.of("attr")
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
		when(idsMan.getAllEntitiesWithContactEmail("email"))
			.thenReturn(Set.of(entity));
		when(entity.getId()).thenReturn(id);
		when(groupMan.isPresent("/A/B")).thenReturn(true);

		restProjectService.addProjectMember("B", "email");

		verify(delGroupMan).addMemberToGroup("/A/B", "/A/B", id);
	}

	@Test
	void shouldNotAddProjectMemberWhenGroupDoesntExist() throws EngineException
	{
		when(groupMan.isPresent("/A/B")).thenReturn(false);

		Assertions.assertThrows(NotFoundException.class, () -> restProjectService.addProjectMember("B", "email"));
	}

	@Test
	void shouldNotAddProjectMemberWhenUserIdentityDoesntExist() throws EngineException
	{
		when(idsMan.getAllEntitiesWithContactEmail("email"))
			.thenThrow(UnknownEmailException.class);
		when(groupMan.isPresent("/A/B")).thenReturn(true);

		Assertions.assertThrows(NotFoundException.class, () -> restProjectService.addProjectMember("B", "email"));
	}

	@Test
	void shouldRemoveProjectMember() throws EngineException
	{
		when(groupMan.isPresent("/A/B")).thenReturn(true);

		restProjectService.removeProjectMember("B", "email");

		verify(groupMan).removeMember("/A/B", new EntityParam(new IdentityTaV(EmailIdentity.ID, "email")));
	}

	@Test
	void shouldGetProjectMember() throws EngineException
	{
		GroupContents groupContents = new GroupContents();
		groupContents.setMembers(List.of(new GroupMembership("/A/B", 2, new Date())));

		when(delGroupMan.getDelegatedGroupMembers("/A/B", "/A/B"))
			.thenReturn(List.of(
				new DelegatedGroupMember(2, "/A/B", "/B", GroupAuthorizationRole.manager,
				"name", new VerifiableElementBase("email@gmail.com"),
					Optional.of(List.of(new Attribute("attr", "string", "/A/B", List.of("val"))))))
			);
		when(groupMan.isPresent("/A/B")).thenReturn(true);

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
		when(groupMan.isPresent("/A/B")).thenReturn(true);

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
		GroupContents groupContents = new GroupContents();
		groupContents.setMembers(List.of(new GroupMembership("/A/B", 2, new Date())));

		when(delGroupMan.getDelegatedGroupMembers("/A/B", "/A/B"))
			.thenReturn(List.of(
				new DelegatedGroupMember(2, "/A/B", "/B", GroupAuthorizationRole.manager,
					"name", new VerifiableElementBase("email@gmail.com"),
					Optional.of(List.of(new Attribute("attr", "string", "/A/B", List.of("val"))))))
			);
		when(groupMan.isPresent("/A/B")).thenReturn(true);

		RestAuthorizationRole role = restProjectService.getProjectAuthorizationRole("B", "email@gmail.com");

		assertThat(role).isEqualTo(new RestAuthorizationRole("manager"));
	}

	@Test
	void shouldSetProjectAuthorizationRole() throws EngineException
	{
		long id = 2L;
		Entity entity = mock(Entity.class);
		when(idsMan.getAllEntitiesWithContactEmail("email"))
			.thenReturn(Set.of(entity));
		when(entity.getId()).thenReturn(id);
		when(groupMan.isPresent("/A/B")).thenReturn(true);


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
}
