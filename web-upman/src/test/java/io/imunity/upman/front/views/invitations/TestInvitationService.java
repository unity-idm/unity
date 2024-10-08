/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.invitations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.imunity.upman.front.model.Group;
import io.imunity.upman.front.model.GroupTreeNode;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.utils.DelegatedGroupsHelper;
import io.imunity.vaadin.elements.NotificationPresenter;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.project.ProjectAddInvitationResult;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;

@ExtendWith(MockitoExtension.class)
public class TestInvitationService
{
	@Mock
	private MessageSource mockMsg;

	@Mock
	private ProjectInvitationsManagement mockInvitationMan;

	@Mock
	private DelegatedGroupsHelper mockDelGroupHelper;

	@Mock
	private NotificationPresenter notificationPresenter;

	private ProjectInvitationsService service;

	@BeforeEach
	public void initController()
	{
		service = new ProjectInvitationsService(mockMsg, mockInvitationMan, mockDelGroupHelper, notificationPresenter);
	}

	@Test
	public void shouldAdd() throws EngineException
	{
		Instant expiration = Instant.now();
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		Group group = new Group("/", new I18nString("group"), "group", false, false, "", false, 0);
		GroupTreeNode node = new GroupTreeNode(group, 0);
		when(mockInvitationMan.addInvitations(any())).thenReturn(ProjectAddInvitationResult.builder()
				.build());
		service.addInvitations(new InvitationRequest(project, List.of("demo@demo.com", "demo2@demo.com"), Set.of(node),
				false, expiration));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Set<ProjectInvitationParam>> argument = ArgumentCaptor.forClass(Set.class);
		verify(mockInvitationMan, times(1)).addInvitations(argument.capture());

		List<Set<ProjectInvitationParam>> arguments = argument.getAllValues();
		assertThat(arguments.get(0)
				.size()).isEqualTo(2);

		assertThat(arguments.get(0)
				.iterator()
				.next().project).isEqualTo("/project");
		assertThat(arguments.get(0)
				.stream()
				.map(p -> p.contactAddress)
				.collect(Collectors.toSet())).isEqualTo(Set.of("demo@demo.com", "demo2@demo.com"));
		assertThat(arguments.get(0)
				.iterator()
				.next().groups.iterator()
						.next()).isEqualTo("/");
		assertThat(arguments.get(0)
				.iterator()
				.next().expiration).isEqualTo(expiration);
	}

	@Test
	public void shouldRemove() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		InvitationModel code = new InvitationModel("code", null, null, null, null, null);

		service.removeInvitations(project, Set.of(code));

		verify(mockInvitationMan).removeInvitation(eq("/project"), eq("code"));

	}

	@Test
	public void shouldSend() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		InvitationModel code = new InvitationModel("code", null, null, null, null, null);

		service.resendInvitations(project, Set.of(code));

		verify(mockInvitationMan).sendInvitation(eq("/project"), eq("code"));
	}

	@Test
	public void shouldGetInvitations() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");

		service.getInvitations(project);

		verify(mockInvitationMan).getInvitations(eq("/project"));
	}
}
