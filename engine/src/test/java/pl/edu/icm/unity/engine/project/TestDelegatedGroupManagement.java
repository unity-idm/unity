/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.Group;
import pl.edu.icm.unity.base.group.GroupContents;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.group.GroupMembership;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.project.SubprojectGroupDelegationConfiguration;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.IllegalGroupAttributeException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.IllegalGroupNameException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.OneManagerRemainsException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.RemovalOfProjectGroupException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.RemovalOfSubProjectGroupException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.RenameProjectGroupException;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.store.api.AttributeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.types.StoredAttribute;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestDelegatedGroupManagement extends TestProjectBase
{
	private DelegatedGroupManagementImpl dGroupManNoAuthz;
	private DelegatedGroupManagementImpl dGroupManWithMockAuthz;
	@Mock
	private GroupDAO mockGroupDao;

	@Mock
	private AttributeDAO mockAttrDao;
	
	@Before
	public void initDelegatedGroupMan() 
	{

		ProjectAuthorizationManager mockAuthz2 = new ProjectAuthorizationManager(mockGroupDao, mockAttrDao);
		dGroupManWithMockAuthz = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, mockBulkQueryService,
				mockAttrTypeMan, mockIdMan, mockAttrHelper, mockRegistrationMan, mockEnquiryMan, mockConfigGenerator,
				new ProjectAttributeHelper(mockAttrMan, mockAttrHelper, mockAtHelper), mockAuthz2);

		dGroupManNoAuthz = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, mockBulkQueryService,
				mockAttrTypeMan, mockIdMan, mockAttrHelper, mockRegistrationMan, mockEnquiryMan, mockConfigGenerator,
				new ProjectAttributeHelper(mockAttrMan, mockAttrHelper, mockAtHelper), mockAuthz);

	}

	@Test
	public void shouldForwardGroupAddToCoreManager() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getGroupContent("/project"));

		I18nString groupName = new I18nString("GroupName");
		dGroupManNoAuthz.addGroup("/project1", "project1/subgroup", groupName, false);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(mockGroupMan).addGroup(argument.capture());
		assertThat(argument.getValue().getDisplayedName(), is(groupName));
	}

	@Test
	public void shoudForbidToAddGroupWithIllegalName() throws EngineException
	{

		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getGroupContent("/project1"));

		Throwable exception = catchThrowable(
				() -> dGroupManNoAuthz.addGroup("/project1", "project1/subgroup", new I18nString(), false));
		assertExceptionType(exception, IllegalGroupNameException.class);
	}

	@Test
	public void shouldForbidRemoveOfProjectGroup() throws EngineException
	{

		Throwable exception = catchThrowable(() -> dGroupManNoAuthz.removeGroup("/project1", "/project1"));
		assertExceptionType(exception, RemovalOfProjectGroupException.class);
	}

	@Test
	public void shouldForwardGroupRemoveToCoreManager() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getGroupContent("/project1"));

		dGroupManNoAuthz.removeGroup("/project1", "/project1/group1");
		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(mockGroupMan).removeGroup(argument.capture(), eq(true));
		assertThat(argument.getValue(), is("/project1/group1"));
	}
	
	@Test
	public void shouldForbidGroupRemoveWhenIsSubprojectGroup() throws EngineException
	{
		when(mockGroupMan.getContents(eq("/project/sub"), anyInt()))
				.thenReturn(getEnabledGroupContentsWithDefaultMember("/project/sub"));

		Throwable exception = catchThrowable(() -> dGroupManNoAuthz.removeGroup("/project", "/project/sub"));
		assertExceptionType(exception, RemovalOfSubProjectGroupException.class);
	}

	
	@Test
	public void shouldForbidSubProjectRemoveWhenIsOnlyMananger() throws EngineException
	{
		setupInvocationContext();
		Attribute baseAttribute = new Attribute(null, null, null, Arrays.asList(GroupAuthorizationRole.manager.toString()));
		when(mockAttrDao.getAttributes(anyString(), any(), eq("/project"))).thenReturn(
			Arrays.asList(new StoredAttribute(new AttributeExt(baseAttribute, false), 1L)));
		Group group = new Group("/project");
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(true, true, null, null, null, null, List.of()));
		when(mockGroupDao.get(eq("/project"))).thenReturn(group);
		Throwable exception = catchThrowable(() -> dGroupManWithMockAuthz.removeProject("/project", "/project/sub"));
		assertExceptionType(exception, AuthorizationException.class);
	}
	
	@Test
	public void shouldForwardSubprojectRemoveToCoreManager() throws EngineException
	{
		setupInvocationContext();

		when(mockAttrDao.getAttributes(anyString(), any(),
				eq("/project")))
						.thenReturn(
								Arrays.asList(
										new StoredAttribute(
												new AttributeExt(
														new Attribute(null, null, null,
																Arrays.asList(
																		GroupAuthorizationRole.projectsAdmin.toString())),
														false),
											1L)));
		Group group = new Group("/project");
		group.setDelegationConfiguration(
				new GroupDelegationConfiguration(true, true, null, null, null, null, List.of()));
		when(mockGroupDao.get(eq("/project"))).thenReturn(group);
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getEnabledGroupContentsWithDefaultMember("/project1"));

		dGroupManWithMockAuthz.removeProject("/project", "/project/sub");
		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(mockGroupMan).removeGroup(argument.capture(), eq(true));
		assertThat(argument.getValue(), is("/project/sub"));
	}
	

	@Test
	public void shouldRemoveFormsWhenRemoveGroup() throws EngineException
	{
		GroupContents con = getEnabledGroupContentsWithDefaultMember("/project");
		con.getGroup().setDelegationConfiguration(new GroupDelegationConfiguration(true, false, null, "reg", "e1", "e2", 
				null));		
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(con);
		dGroupManNoAuthz.removeProject("/project1", "/project1/group1");
		verify(mockRegistrationMan).removeFormWithoutDependencyChecking(eq("reg"));
		verify(mockEnquiryMan).removeEnquiryWithoutDependencyChecking(eq("e2"));
		verify(mockEnquiryMan).removeEnquiryWithoutDependencyChecking(eq("e2"));
	}
	
	@Test
	public void shouldSkipRemoveFormsWhenRemoveGroupAndDelegationIfNotActive() throws EngineException
	{
		GroupContents con = getEnabledGroupContentsWithDefaultMember("/project");
		con.getGroup().setDelegationConfiguration(
				new GroupDelegationConfiguration(false, false, null, "reg", "e1", "e2", null));
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(con);
		dGroupManNoAuthz.removeGroup("/project1", "/project1/group1");
		verify(mockRegistrationMan, never()).removeFormWithoutDependencyChecking(eq("reg"));
		verify(mockEnquiryMan, never()).removeEnquiryWithoutDependencyChecking(eq("e2"));
		verify(mockEnquiryMan, never()).removeEnquiryWithoutDependencyChecking(eq("e2"));
	}

	@Test
	public void shouldReturnGroupAndSubgroups() throws EngineException
	{

		when(mockBulkQueryService.getBulkStructuralData(anyString())).thenReturn(null);
		Map<String, GroupContents> groupsWithSubgroups = new HashMap<>();
		groupsWithSubgroups.put("/project", getGroupContent("/project", List.of("/project/subgroup")));
		groupsWithSubgroups.put("/project/subgroup", getGroupContent("/project/subgroup"));
		when(mockBulkQueryService.getGroupAndSubgroups(any())).thenReturn(groupsWithSubgroups);
		Map<String, DelegatedGroupContents> groupAndSubgroups = dGroupManNoAuthz.getGroupAndSubgroups("/project",
				"/project");

		assertThat(groupAndSubgroups.size(), is(2));
		assertThat(groupAndSubgroups.get("/project").group.path, is("/project"));
		assertThat(groupAndSubgroups.get("/project/subgroup").group.path, is("/project/subgroup"));
	}

	@Test
	public void shouldGetGroupContents() throws EngineException
	{
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getGroupContent("/project/subGroup"));

		DelegatedGroupContents contents = dGroupManNoAuthz.getContents("/project", "/project/subGroup");

		assertThat(contents.group.path, is("/project/subGroup"));
	}

	@Test
	public void shouldGetGroupMembersWithExtraAttrs() throws EngineException
	{

		GroupContents con = getEnabledGroupContentsWithDefaultMember("/project");
		con.getGroup().setDelegationConfiguration(new GroupDelegationConfiguration(true, false, null, null, null, null,
				Arrays.asList("extraAttr")));

		when(mockIdMan.getEntity(any()))
				.thenReturn(new Entity(
						Arrays.asList(new Identity(EmailIdentity.ID, "demo@demo.com", 1,
								new UsernameIdentity().getComparableValue("", "", ""))),
						null, null));

		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(con);
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(con);

		when(mockAttrHelper.getAttributeTypeWithSingeltonMetadata(eq(EntityNameMetadataProvider.NAME)))
				.thenReturn(new AttributeType("name", null));
		
		when(mockAtHelper.getUnconfiguredSyntaxForAttributeName(eq("name")))
				.thenAnswer(x -> new StringAttributeSyntax());

		when(mockAttrMan.getAttributes(any(), eq("/project"), eq(
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE)))
						.thenReturn(Arrays.asList(getAttributeExt(
								GroupAuthorizationRole.manager.toString())));

		when(mockAttrMan.getAttributes(any(), eq("/"), eq("name")))
				.thenReturn(Arrays.asList(getAttributeExt("demo")));

		when(mockAttrMan.getAttributes(any(), eq("/project"), eq("extraAttr")))
				.thenReturn(Arrays.asList(getAttributeExt("extraValue")));

		List<DelegatedGroupMember> delegatedGroupMemebers = dGroupManNoAuthz.getDelegatedGroupMembers("/project",
				"/project");

		assertThat(delegatedGroupMemebers.size(), is(1));

		DelegatedGroupMember firstMember = delegatedGroupMemebers.iterator().next();
		assertThat(firstMember.entityId, is(1L));
		assertThat(firstMember.email.getValue(), is("demo@demo.com"));
		assertThat(firstMember.name, is("demo"));
		assertThat(firstMember.attributes.iterator().next().getValues().iterator().next(), is("extraValue"));

	}

	@Test
	public void shouldForbidGetDisplayNameOfNonProjectAttribute() throws EngineException
	{
		GroupContents contents = getGroupContent("/project");
		contents.getGroup().setDelegationConfiguration(new GroupDelegationConfiguration(true, false, null, null, null,
				null, Arrays.asList("extraAttr")));
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(contents);

		Throwable exception = catchThrowable(() -> dGroupManNoAuthz.getAttributeDisplayedName("/project", "demo"));
		assertExceptionType(exception, IllegalGroupAttributeException.class);
	}

	@Test
	public void shouldForbidRenameProjectGroup()
	{

		Throwable exception = catchThrowable(
				() -> dGroupManNoAuthz.setGroupDisplayedName("/project", "/project", null));
		assertExceptionType(exception, RenameProjectGroupException.class);
	}

	@Test
	public void shouldForwardRenameGroupToCoreManager() throws EngineException
	{

		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getGroupContent("/project"));

		I18nString newName = new I18nString("demoName");
		dGroupManNoAuthz.setGroupDisplayedName("/project", "/project/subgroup", newName);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(mockGroupMan).updateGroup(eq("/project/subgroup"), argument.capture(), eq("set displayed name"), eq("demoName"));
		assertThat(argument.getValue().getDisplayedName(), is(newName));
	}

	@Test
	public void shouldForwardUpdateGroupAccessModeToCoreManager() throws EngineException
	{
		GroupContents content = getGroupContent("/project",
				Arrays.asList("/project/subgroup", "/project/subgroup/subgroup2"));
		content.getGroup().setPublic(true);
		
		when(mockGroupMan.getContents(eq("/project/subgroup"), anyInt())).thenReturn(
				getGroupContent("/project/subgroup", Arrays.asList("/project/subgroup/subgroup2")));

		dGroupManNoAuthz.setGroupAccessMode("/project", "/project/subgroup", true);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(mockGroupMan).updateGroup(eq("/project/subgroup"), argument.capture(), eq("set access mode"), eq("public"));
		assertThat(argument.getValue().isPublic(), is(true));
	}

	@Test
	public void shouldForwardSetGroupAuthAttributeToCoreManager() throws EngineException
	{

		dGroupManNoAuthz.setGroupAuthorizationRole("/project", "/project", 1L, GroupAuthorizationRole.manager);

		ArgumentCaptor<Attribute> argument = ArgumentCaptor.forClass(Attribute.class);
		verify(mockAttrHelper).addSystemAttribute(eq(1L), argument.capture(), eq(true));

		assertThat(argument.getValue().getValues().iterator().next(),
				is(GroupAuthorizationRole.manager.toString()));
	}

	@Test
	public void shouldForbidRemoveLastManagerInProjectGroup() throws EngineException
	{

		when(mockIdMan.getEntity(any()))
		.thenReturn(new Entity(
				Arrays.asList(new Identity(UsernameIdentity.ID, "xx", 1,
						new UsernameIdentity().getComparableValue("", "", ""))),
				null, null));
		
		when(mockGroupMan.getContents(eq("/project"), anyInt()))
				.thenReturn(getEnabledGroupContentsWithDefaultMember("/project"));

		when(mockAttrMan.getAttributes(any(), eq("/project"), eq(
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE)))
						.thenReturn(Arrays.asList(getAttributeExt(
								GroupAuthorizationRole.manager.toString())));

		Throwable exception = catchThrowable(() -> dGroupManNoAuthz.setGroupAuthorizationRole("/project", "/project", 1L,
				GroupAuthorizationRole.regular));
		assertExceptionType(exception, OneManagerRemainsException.class);
	}

	@Test
	public void shouldGetOneProjectForEntity() throws EngineException
	{

		Map<String, GroupMembership> groups = new HashMap<>();
		groups.put("/project", null);
		when(mockIdMan.getGroups(any())).thenReturn(groups);

		when(mockGroupMan.getContents(eq("/project"), anyInt()))
				.thenReturn(getEnabledGroupContentsWithDefaultMember("/project"));

		when(mockAttrMan.getAttributes(any(), eq("/project"), eq(
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE)))
						.thenReturn(Arrays.asList(getAttributeExt(
								GroupAuthorizationRole.manager.toString())));

		List<DelegatedGroup> projectsForEntity = dGroupManNoAuthz.getProjectsForEntity(1L);

		assertThat(projectsForEntity.size(), is(1));
		assertThat(projectsForEntity.iterator().next().path, is("/project"));
	}

	@Test
	public void shouldForwardAddMemberToCoreManager() throws EngineException
	{

		Map<String, GroupMembership> groups = new HashMap<>();
		groups.put("/project", null);
		when(mockIdMan.getGroups(any())).thenReturn(groups);

		dGroupManNoAuthz.addMemberToGroup("/project", "/project/destination", 1L);

		verify(mockGroupMan).addMemberFromParent(eq("/project/destination"), any());
	}

	@Test
	public void shouldForwardRemoveMemberToCoreManager() throws EngineException
	{

		dGroupManNoAuthz.removeMemberFromGroup("/project", "/project/destination", 1L);
		verify(mockGroupMan).removeMember(eq("/project/destination"), any());
	}
	
	@Test
	public void shouldForwardSetGroupDelegationConfigToCoreManager() throws EngineException
	{
		when(mockGroupMan.getContents(eq("/project"), anyInt()))
				.thenReturn(getEnabledGroupContentsWithDefaultMember("/project"));

		when(mockGroupMan.getContents(eq("/project/sub"), anyInt()))
				.thenReturn(getGroupContent("/project/sub"));

		when(mockConfigGenerator.generateSubprojectRegistrationForm(any(), eq("/project"), eq("/project/sub"),
				eq("https://test/test.jpg"))).thenReturn(
						new RegistrationFormBuilder().withDefaultCredentialRequirement(
								EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
								.withName("test").build());

		when(mockConfigGenerator.generateSubprojectJoinEnquiryForm(any(), eq("/project"), eq("/project/sub"),
				eq("https://test/test.jpg"))).thenReturn(
						new EnquiryFormBuilder().withTargetGroups(new String[] { "/" })
								.withType(EnquiryForm.EnquiryType.STICKY)
								.withName("test").build());

		when(mockConfigGenerator.generateSubprojectUpdateEnquiryForm(any(), eq("/project"), eq("/project/sub"),
				eq("https://test/test.jpg"))).thenReturn(
						new EnquiryFormBuilder().withTargetGroups(new String[] { "/" })
								.withType(EnquiryForm.EnquiryType.STICKY)
								.withName("test").build());

		dGroupManNoAuthz.setGroupDelegationConfiguration("/project", "/project/sub", new SubprojectGroupDelegationConfiguration(true, false, "https://test/test.jpg"));

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(mockGroupMan).updateGroup(eq("/project/sub"), argument.capture());

		assertThat(argument.getValue().getDelegationConfiguration().enabled, is(true));

		assertThat(argument.getValue().getDelegationConfiguration().logoUrl, is("https://test/test.jpg"));
	}

	private AttributeExt getAttributeExt(String value)
	{
		return new AttributeExt(new Attribute(null, null, null, Arrays.asList(value)), false);
	}

	private GroupContents getGroupContent(String path, List<String> subgroups)
	{
		GroupContents con = new GroupContents();
		con.setGroup(new Group(path));
		con.setSubGroups(subgroups);
		return con;
	}

	private GroupContents getGroupContent(String path)
	{
		return getGroupContent(path, List.of());
	}

	private GroupContents getEnabledGroupContentsWithDefaultMember(String path)
	{
		GroupContents content = getConfiguredGroupContents(path);
		GroupMembership member = new GroupMembership("/project", 1L, new Date());
		content.setMembers(List.of(member));
		return content;
	}
	
	private void setupInvocationContext()
	{
		InvocationContext invContext = new InvocationContext(null, null, null);
		invContext.setLoginSession(new LoginSession("1", null, null, 100, 1L, null, null, null, null));
		InvocationContext.setCurrent(invContext);
	}

}
