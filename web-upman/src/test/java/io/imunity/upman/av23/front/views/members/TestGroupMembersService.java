/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.members;

import com.google.common.collect.Sets;
import io.imunity.upman.av23.front.components.NotificationPresenter;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.ProjectGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestGroupMembersService
{
	@Mock
	private MessageSource mockMsg;
	@Mock
	private DelegatedGroupManagement mockDelGroupMan;
	@Mock
	private AttributeHandlerRegistry mockAttrHandlerRegistry;
	@Mock
	private NotificationPresenter notificationPresenter;

	private GroupMembersService service;

	@Before
	public void initController()
	{
		service = new GroupMembersService(mockMsg, mockAttrHandlerRegistry, mockDelGroupMan, notificationPresenter);
	}

	@Test
	public void shouldForwardGerMembersToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		Group group = new Group("/project/group", "group", false, false, "", false, 0);

		service.getGroupMembers(project, group);
		verify(mockDelGroupMan).getDelegatedGroupMemebers(eq("/project"), eq("/project/group"));

	}

	@Test
	public void shouldForwardAddMemberToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		Group group = new Group("/project/group", "group", false, false, "", false, 0);

		service.addToGroup(project, List.of(group), Set.of(getMember()));
		verify(mockDelGroupMan).addMemberToGroup(eq("/project"), eq("/project/group"), eq(1L));
	}

	@Test
	public void shouldForwardRemoveMemberToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		Group group = new Group("/project/group", "group", false, false, "", false, 0);

		service.removeFromGroup(project, group, Set.of(getMember()));
		verify(mockDelGroupMan).removeMemberFromGroup(eq("/project"), eq("/project/group"), eq(1L));

	}

	@Test
	public void shouldForwardSetRoleToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		Group group = new Group("/project/group", "group", false, false, "", false, 0);

		service.updateRole(project, group,  GroupAuthorizationRole.manager, Sets.newHashSet(getMember()));
		verify(mockDelGroupMan).setGroupAuthorizationRole(eq("/project"), eq("/project/group"), eq(1L),
				eq(GroupAuthorizationRole.manager));

	}

	@Test
	public void shouldForwardGetAdditinalAttributesToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");

		DelegatedGroup delGroup = new DelegatedGroup("/project", new GroupDelegationConfiguration(true, false, null,
				null, null, null, List.of("extraAttr")), true, "name");

		DelegatedGroupContents con = new DelegatedGroupContents(delGroup, Optional.empty());

		when(mockDelGroupMan.getContents(eq("/project"), eq("/project"))).thenReturn(con);
		when(mockDelGroupMan.getAttributeDisplayedName(eq("/project"), eq("extraAttr"))).thenReturn("extra");

		Map<String, String> additionalAttributeNamesForProject = service
				.getAdditionalAttributeNamesForProject(project);
		assertThat(additionalAttributeNamesForProject.isEmpty(), is(false));
		assertThat(additionalAttributeNamesForProject.get("extraAttr"), is("extra"));

	}

	private MemberModel getMember()
	{
		return MemberModel.builder()
				.entityId(1)
				.build();
	}

}
