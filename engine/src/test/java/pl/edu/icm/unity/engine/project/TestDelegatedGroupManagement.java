/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.project;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.IllegalGroupAttributeException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.IllegalGroupNameException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.OneManagerRemainsException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.OpenChildGroupException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.ParentIsCloseGroupException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.RemovalOfProjectGroupException;
import pl.edu.icm.unity.engine.project.DelegatedGroupManagementImpl.RenameProjectGroupException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.stdext.attr.VerifiableEmailAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.ContactEmailMetadataProvider;
import pl.edu.icm.unity.stdext.utils.EntityNameMetadataProvider;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.types.basic.VerifiableEmail;

@RunWith(MockitoJUnitRunner.class)
public class TestDelegatedGroupManagement
{
	@Mock
	ProjectAuthorizationManager mockAuthz;

	@Mock
	GroupsManagement mockGroupMan;

	@Mock
	BulkGroupQueryService mockBulkQueryService;

	@Mock
	UnityMessageSource mockMsg;

	@Mock
	AttributesManagement attrMan;

	@Mock
	AttributeTypeManagement attrTypeMan;

	@Mock
	AttributesHelper attrHelper;

	@Mock
	AttributeTypeHelper atHelper;

	@Mock
	EntityManagement idMan;

	@Test
	public void shouldAddGroup() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(null, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getGroupContent("/project"));

		I18nString groupName = new I18nString("GroupName");
		dGroupMan.addGroup("/project1", "project1/subgroup", groupName, false);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(mockGroupMan).addGroup(argument.capture());
		assertThat(argument.getValue().getDisplayedName(), is(groupName));
	}
	
	@Test
	public void shouldThrowIllegalGroupName() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(null, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getGroupContent("/project"));

		Throwable exception = catchThrowable(
				() -> dGroupMan.addGroup("/project1", "project1/subgroup", new I18nString(), false));
		assertException(exception, IllegalGroupNameException.class);
	}

	@Test
	public void shouldThrowRemovalOfProjectGroupException() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(null, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		Throwable exception = catchThrowable(() -> dGroupMan.removeGroup("/project1", "/project1"));
		assertException(exception, RemovalOfProjectGroupException.class);
	}

	@Test
	public void shouldRemoveGroup() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(null, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		dGroupMan.removeGroup("/project1", "/project1/group1");

		ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
		verify(mockGroupMan).removeGroup(argument.capture(), eq(true));
		assertThat(argument.getValue(), is("/project1/group1"));
	}

	@Test
	public void shouldReturnGroupAndSubgroups() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, null,
				mockBulkQueryService, null, null, null, null, null, mockAuthz);

		when(mockBulkQueryService.getBulkStructuralData(anyString())).thenReturn(null);
		Map<String, GroupContents> groupsWithSubgroups = new HashMap<>();
		groupsWithSubgroups.put("/project", getGroupContent("/project", Lists.list("/project/subgroup")));
		groupsWithSubgroups.put("/project/subgroup", getGroupContent("/project/subgroup"));
		when(mockBulkQueryService.getGroupAndSubgroups(any())).thenReturn(groupsWithSubgroups);
		Map<String, DelegatedGroupContents> groupAndSubgroups = dGroupMan.getGroupAndSubgroups("/project",
				"/project");

		assertThat(groupAndSubgroups.size(), is(2));
		assertThat(groupAndSubgroups.get("/project").group.path, is("/project"));
		assertThat(groupAndSubgroups.get("/project/subgroup").group.path, is("/project/subgroup"));
	}

	@Test
	public void shouldGetGroupContents() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getGroupContent("/project/subGroup"));

		DelegatedGroupContents contents = dGroupMan.getContents("/project", "/project/subGroup");

		assertThat(contents.group.path, is("/project/subGroup"));
	}

	@Test
	public void shouldGetGroupMembersWithExtraAttrs() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				attrMan, attrTypeMan, null, attrHelper, atHelper, mockAuthz);

		GroupContents con = getEnabledGroupContentsWithDefaultMember("/project");
		con.getGroup().setDelegationConfiguration(new GroupDelegationConfiguration(true, null, null, null, null,
				Arrays.asList("extraAttr")));

		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(con);
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(con);

		when(attrHelper.getAttributeTypeWithSingeltonMetadata(eq(EntityNameMetadataProvider.NAME)))
				.thenReturn(new AttributeType("name", null));
		when(attrHelper.getAttributeTypeWithSingeltonMetadata(eq(ContactEmailMetadataProvider.NAME)))
				.thenReturn(new AttributeType("email", null));

		when(atHelper.getUnconfiguredSyntaxForAttributeName(eq("name")))
				.thenAnswer(x -> new StringAttributeSyntax());

		when(atHelper.getUnconfiguredSyntaxForAttributeName(eq("email")))
				.thenAnswer(x -> new VerifiableEmailAttributeSyntax());

		when(attrMan.getAttributes(any(), eq("/project"), eq(
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE)))
						.thenReturn(Arrays.asList(getAttributeExt(
								GroupAuthorizationRole.manager.toString())));

		when(attrMan.getAttributes(any(), eq("/project"), eq("email"))).thenReturn(
				Arrays.asList(getAttributeExt(new VerifiableEmail("demo@demo.com").toJsonString())));

		when(attrMan.getAttributes(any(), eq("/project"), eq("name")))
				.thenReturn(Arrays.asList(getAttributeExt("demo")));

		when(attrMan.getAttributes(any(), eq("/project"), eq("extraAttr")))
				.thenReturn(Arrays.asList(getAttributeExt("extraValue")));

		List<DelegatedGroupMember> delegatedGroupMemebers = dGroupMan.getDelegatedGroupMemebers("/project",
				"/project");

		assertThat(delegatedGroupMemebers.size(), is(1));

		DelegatedGroupMember firstMember = delegatedGroupMemebers.iterator().next();
		assertThat(firstMember.entityId, is(1L));
		assertThat(firstMember.email, is("demo@demo.com"));
		assertThat(firstMember.name, is("demo"));
		assertThat(firstMember.attributes.iterator().next().getValues().iterator().next(), is("extraValue"));

	}

	@Test
	public void shouldThrowIllegalGroupAttributeException() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);
		GroupContents contents = getGroupContent("/project");
		contents.getGroup().setDelegationConfiguration(new GroupDelegationConfiguration(true, null, null, null,
				null, Arrays.asList("extraAttr")));
		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(contents);

		Throwable exception = catchThrowable(() -> dGroupMan.getAttributeDisplayedName("/project", "demo"));
		assertException(exception, IllegalGroupAttributeException.class);
	}

	@Test
	public void shouldThrowRenameProjectGroupException()
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		Throwable exception = catchThrowable(
				() -> dGroupMan.setGroupDisplayedName("/project", "/project", null));
		assertException(exception, RenameProjectGroupException.class);
	}

	@Test
	public void shouldRenameGroup() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		when(mockGroupMan.getContents(any(), anyInt())).thenReturn(getGroupContent("/project"));

		I18nString newName = new I18nString("demoName");
		dGroupMan.setGroupDisplayedName("/project", "/project/subgroup", newName);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(mockGroupMan).updateGroup(eq("/project/subgroup"), argument.capture());
		assertThat(argument.getValue().getDisplayedName(), is(newName));
	}

	@Test
	public void shouldThrowOpenChildGroupException() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		when(mockGroupMan.getContents(eq("/project/subgroup"), anyInt())).thenReturn(
				getGroupContent("/project/subgroup", Arrays.asList("/project/subgroup/subgroup2")));

		GroupContents childCon2 = getGroupContent("/project/subgroup/subgroup2");
		childCon2.getGroup().setOpen(true);

		when(mockGroupMan.getContents(eq("/project/subgroup/subgroup2"), anyInt())).thenReturn(childCon2);

		Throwable exception = catchThrowable(
				() -> dGroupMan.setGroupAccessMode("/project", "/project/subgroup", false));
		assertException(exception, OpenChildGroupException.class);
	}

	@Test
	public void shouldThrowParentIsCloseGroupException() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		GroupContents content = getGroupContent("/project",
				Arrays.asList("/project/subgroup", "/project/subgroup/subgroup2"));

		when(mockGroupMan.getContents(eq("/project"), anyInt())).thenReturn(content);

		when(mockGroupMan.getContents(eq("/project/subgroup"), anyInt())).thenReturn(
				getGroupContent("/project/subgroup", Arrays.asList("/project/subgroup/subgroup2")));

		Throwable exception = catchThrowable(
				() -> dGroupMan.setGroupAccessMode("/project", "/project/subgroup", true));
		assertException(exception, ParentIsCloseGroupException.class);
	}

	@Test
	public void shouldOpenGroup() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);
		GroupContents content = getGroupContent("/project",
				Arrays.asList("/project/subgroup", "/project/subgroup/subgroup2"));
		content.getGroup().setOpen(true);

		when(mockGroupMan.getContents(eq("/project"), anyInt())).thenReturn(content);

		when(mockGroupMan.getContents(eq("/project/subgroup"), anyInt())).thenReturn(
				getGroupContent("/project/subgroup", Arrays.asList("/project/subgroup/subgroup2")));

		dGroupMan.setGroupAccessMode("/project", "/project/subgroup", true);

		ArgumentCaptor<Group> argument = ArgumentCaptor.forClass(Group.class);
		verify(mockGroupMan).updateGroup(eq("/project/subgroup"), argument.capture());
		assertThat(argument.getValue().isOpen(), is(true));
	}

	@Test
	public void shouldSetManagerRole() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, null, attrHelper, null, mockAuthz);

		dGroupMan.setGroupAuthorizationRole("/project", 1L, GroupAuthorizationRole.manager);

		ArgumentCaptor<Attribute> argument = ArgumentCaptor.forClass(Attribute.class);
		verify(attrHelper).addSystemAttribute(eq(1L), argument.capture(), eq(true));

		assertThat(argument.getValue().getValues().iterator().next(),
				is(GroupAuthorizationRole.manager.toString()));
	}

	@Test
	public void shouldThrowOneManagerRemainsException() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				attrMan, attrTypeMan, null, attrHelper, atHelper, mockAuthz);

		when(mockGroupMan.getContents(eq("/project"), anyInt()))
				.thenReturn(getEnabledGroupContentsWithDefaultMember("/project"));

		when(attrMan.getAttributes(any(), eq("/project"), eq(
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE)))
						.thenReturn(Arrays.asList(getAttributeExt(
								GroupAuthorizationRole.manager.toString())));

		Throwable exception = catchThrowable(() -> dGroupMan.setGroupAuthorizationRole("/project", 1L,
				GroupAuthorizationRole.regular));
		assertException(exception, OneManagerRemainsException.class);
	}

	@Test
	public void shouldGetOneProjectForEntity() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				attrMan, null, idMan, null, null, mockAuthz);

		Map<String, GroupMembership> groups = new HashMap<>();
		groups.put("/project", null);
		when(idMan.getGroups(any())).thenReturn(groups);

		when(mockGroupMan.getContents(eq("/project"), anyInt()))
				.thenReturn(getEnabledGroupContentsWithDefaultMember("/project"));

		when(attrMan.getAttributes(any(), eq("/project"), eq(
				ProjectAuthorizationRoleAttributeTypeProvider.PROJECT_MANAGEMENT_AUTHORIZATION_ROLE)))
						.thenReturn(Arrays.asList(getAttributeExt(
								GroupAuthorizationRole.manager.toString())));

		List<DelegatedGroup> projectsForEntity = dGroupMan.getProjectsForEntity(1L);

		assertThat(projectsForEntity.size(), is(1));
		assertThat(projectsForEntity.iterator().next().path, is("/project"));
	}

	@Test
	public void shouldAddMember() throws EngineException
	{

		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, idMan, null, null, mockAuthz);
		Map<String, GroupMembership> groups = new HashMap<>();
		groups.put("/project", null);
		when(idMan.getGroups(any())).thenReturn(groups);

		dGroupMan.addMemberToGroup("/project", "/project/destination", 1L);

		verify(mockGroupMan).addMemberFromParent(eq("/project/destination"), any());
	}

	@Test
	public void shouldRemoveMember() throws EngineException
	{
		DelegatedGroupManagementImpl dGroupMan = new DelegatedGroupManagementImpl(mockMsg, mockGroupMan, null,
				null, null, null, null, null, mockAuthz);

		dGroupMan.removeMemberFromGroup("/project", "/project/destination", 1L);
		verify(mockGroupMan).removeMember(eq("/project/destination"), any());
	}

	private void assertException(Throwable exception, Class<?> type)
	{
		Assertions.assertThat(exception).isNotNull().isInstanceOf(type);
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
		return getGroupContent(path, Lists.emptyList());
	}

	private GroupContents getEnabledGroupContentsWithDefaultMember(String path)
	{
		GroupContents content = getGroupContent("/project");
		GroupMembership member = new GroupMembership("/project", 1L, new Date());
		content.setMembers(Lists.list(member));
		content.getGroup().setDelegationConfiguration(
				new GroupDelegationConfiguration(true, null, null, null, null, Lists.emptyList()));
		return content;
	}
}
