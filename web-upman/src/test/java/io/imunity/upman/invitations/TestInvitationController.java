/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;

import io.imunity.upman.utils.DelegatedGroupsHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationParam;
import pl.edu.icm.unity.engine.api.project.ProjectInvitationsManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TestInvitationController
{
	@Mock
	private UnityMessageSource mockMsg;

	@Mock
	private ProjectInvitationsManagement mockInvitationMan;

	@Mock
	private DelegatedGroupsHelper mockDelGroupHelper;

	private InvitationsController controller;

	@Before
	public void initController()
	{
		controller = new InvitationsController(mockMsg, mockInvitationMan, mockDelGroupHelper);
	}

	@Test
	public void shouldForwardAddToCoreManager() throws ControllerException, EngineException
	{
		Instant expiration = Instant.now();
		controller.addInvitation(new ProjectInvitationParam("/project", "demo@demo.com", Arrays.asList("/"),
				expiration));

		ArgumentCaptor<ProjectInvitationParam> argument = ArgumentCaptor.forClass(ProjectInvitationParam.class);
		verify(mockInvitationMan).addInvitation(argument.capture());
		assertThat(argument.getValue().project, is("/project"));
		assertThat(argument.getValue().contactAddress, is("demo@demo.com"));
		assertThat(argument.getValue().allowedGroup.iterator().next(), is("/"));
		assertThat(argument.getValue().expiration, is(expiration));
	}

	@Test
	public void shouldForwardRemoveToCoreManager() throws ControllerException, EngineException
	{
		controller.removeInvitations("/project",
				Sets.newHashSet(new InvitationEntry("code", null, null, null, null, null)));

		verify(mockInvitationMan).removeInvitation(eq("/project"), eq("code"));

	}

	@Test
	public void shouldForwardSendToCoreManager() throws ControllerException, EngineException
	{
		controller.resendInvitations("/project",
				Sets.newHashSet(new InvitationEntry("code", null, null, null, null, null)));
		verify(mockInvitationMan).sendInvitation(eq("/project"), eq("code"));
	}

	@Test
	public void shouldForwardGetInvToCoreManager() throws ControllerException, EngineException
	{
		controller.getInvitations("/project");
		verify(mockInvitationMan).getInvitations(eq("/project"));
	}

	@Test
	public void shouldForwardGetProjectGroupsToHelper() throws ControllerException, EngineException
	{
		controller.getProjectGroups("/project");
		verify(mockDelGroupHelper).getProjectGroups(eq("/project"));
	}
}
