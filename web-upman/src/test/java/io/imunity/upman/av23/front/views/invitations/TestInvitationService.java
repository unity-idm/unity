/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.invitations;

import io.imunity.upman.av23.front.components.NotificationPresenter;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.GroupTreeNode;
import io.imunity.upman.av23.front.model.ProjectGroup;
import io.imunity.upman.utils.DelegatedGroupsHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.exceptions.EngineException;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
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

	private InvitationsService service;

	@Before
	public void initController()
	{
		service = new InvitationsService(mockMsg, mockInvitationMan, mockDelGroupHelper, notificationPresenter);
	}

	@Test
	public void shouldForwardAddToCoreManager() throws EngineException
	{
		Instant expiration = Instant.now();
		ProjectGroup project = new ProjectGroup("/project", "project");
		Group group = new Group("/", "group", false, false, "", false, 0);
		GroupTreeNode node = new GroupTreeNode(group, 0);
		service.addInvitations(new InvitationRequest(project, Set.of("demo@demo.com"), Set.of(node), false, expiration));

		ArgumentCaptor<ProjectInvitationParam> argument = ArgumentCaptor.forClass(ProjectInvitationParam.class);
		verify(mockInvitationMan, times(1)).addInvitation(argument.capture());

		List<ProjectInvitationParam> arguments = argument.getAllValues();
		assertThat(arguments.get(0).project, is("/project"));
		assertThat(arguments.get(0).contactAddress, is("demo@demo.com"));
		assertThat(arguments.get(0).groups.iterator().next(), is("/"));
		assertThat(arguments.get(0).expiration, is(expiration));
	}

	@Test
	public void shouldForwardRemoveToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		InvitationModel code = new InvitationModel("code", null, null, null, null, null);

		service.removeInvitations(project, Set.of(code));

		verify(mockInvitationMan).removeInvitation(eq("/project"), eq("code"));

	}

	@Test
	public void shouldForwardSendToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		InvitationModel code = new InvitationModel("code", null, null, null, null, null);

		service.resendInvitations(project, Set.of(code));

		verify(mockInvitationMan).sendInvitation(eq("/project"), eq("code"));
	}

	@Test
	public void shouldForwardGetInvToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");

		service.getInvitations(project);

		verify(mockInvitationMan).getInvitations(eq("/project"));
	}
}
