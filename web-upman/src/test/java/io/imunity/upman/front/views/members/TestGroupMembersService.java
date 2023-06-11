/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.members;

import com.google.common.collect.Sets;
import io.imunity.upman.front.model.Group;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.group.GroupDelegationConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistryV8;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TestGroupMembersService
{
	@Mock
	private MessageSource mockMsg;
	@Mock
	private DelegatedGroupManagement mockDelGroupMan;
	@Mock
	private AttributeHandlerRegistryV8 mockAttrHandlerRegistry;
	@Mock
	private NotificationPresenter notificationPresenter;

	private GroupMembersService service;

	@BeforeEach
	public void initController()
	{
		service = new GroupMembersService(mockMsg, mockAttrHandlerRegistry, mockDelGroupMan, notificationPresenter);
	}

	@Test
	public void shouldGerMembers() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		Group group = new Group("/project/group",  new I18nString("group"),"group", false, false, "", false, 0);

		service.getGroupMembers(project, group);
		verify(mockDelGroupMan).getDelegatedGroupMembers(eq("/project"), eq("/project/group"));

	}

	@Test
	public void shouldAddMember() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		Group group = new Group("/project/group", new I18nString("group"), "group", false, false, "", false, 0);

		service.addToGroup(project, List.of(group), Set.of(getMember()));
		verify(mockDelGroupMan).addMemberToGroup(eq("/project"), eq("/project/group"), eq(1L));
	}

	@Test
	public void shouldRemoveMember() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		Group group = new Group("/project/group", new I18nString("group"), "group", false, false, "", false, 0);

		service.removeFromGroup(project, group, Set.of(getMember()));
		verify(mockDelGroupMan).removeMemberFromGroup(eq("/project"), eq("/project/group"), eq(1L));

	}

	@Test
	public void shouldSetRole() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		Group group = new Group("/project/group", new I18nString("group"), "group", false, false, "", false, 0);

		service.updateRole(project, group,  GroupAuthorizationRole.manager, Sets.newHashSet(getMember()));
		verify(mockDelGroupMan).setGroupAuthorizationRole(eq("/project"), eq("/project/group"), eq(1L),
				eq(GroupAuthorizationRole.manager));

	}

	@Test
	public void shouldGetAdditinalAttributes() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");

		DelegatedGroup delGroup = new DelegatedGroup("/project", new GroupDelegationConfiguration(true, false, null,
				null, null, null, List.of("extraAttr")), true, new I18nString("name"));

		DelegatedGroupContents con = new DelegatedGroupContents(delGroup, Optional.empty());

		when(mockDelGroupMan.getContents(eq("/project"), eq("/project"))).thenReturn(con);
		when(mockDelGroupMan.getAttributeDisplayedName(eq("/project"), eq("extraAttr"))).thenReturn("extra");

		Map<String, String> additionalAttributeNamesForProject = service
				.getAdditionalAttributeNamesForProject(project);
		assertThat(additionalAttributeNamesForProject.isEmpty()).isFalse();
		assertThat(additionalAttributeNamesForProject.get("extraAttr")).isEqualTo("extra");

	}

	private MemberModel getMember()
	{
		return MemberModel.builder()
				.entityId(1)
				.build();
	}

}
