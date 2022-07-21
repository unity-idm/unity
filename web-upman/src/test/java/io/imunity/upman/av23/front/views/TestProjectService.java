/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views;

import io.imunity.upman.av23.components.ProjectService;
import io.imunity.upman.av23.components.Vaddin23WebLogoutHandler;
import io.imunity.upman.av23.front.components.NotificationPresenter;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.ProjectGroup;
import io.imunity.upman.utils.DelegatedGroupsHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestProjectService
{
	@Mock
	private MessageSource msg;
	@Mock
	private DelegatedGroupManagement delGroupMan;
	@Mock
	private DelegatedGroupsHelper delGroupHelper;
	@Mock
	private Vaddin23WebLogoutHandler logoutHandler;
	@Mock
	private NotificationPresenter notificationPresenter;

	private ProjectService projectService;

	@Before
	public void initController()
	{
		projectService = new ProjectService(msg, delGroupMan, logoutHandler, delGroupHelper, notificationPresenter);
	}

	@Test
	public void shouldGetProjectForUser() throws EngineException
	{
		long entityId = 1;

		when(delGroupMan.getProjectsForEntity(entityId)).thenReturn(List.of(new DelegatedGroup("path", new GroupDelegationConfiguration(true), true, "name")));

		List<ProjectGroup> projectForUser = projectService.getProjectForUser(entityId);

		assertEquals(List.of(new ProjectGroup("path", "name")), projectForUser);
	}

	@Test
	public void shouldGetProjectLogo() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");

		GroupDelegationConfiguration configuration = new GroupDelegationConfiguration(true, true, "url", "", "", "", List.of());
		when(delGroupMan.getContents(project.path, project.path)).thenReturn(new DelegatedGroupContents(new DelegatedGroup("path", configuration, true, "name"), Optional.empty()));

		String url = projectService.getProjectLogo(project);

		assertEquals("url", url);
	}

	@Test
	public void shouldGetProjectGroup() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");

		GroupDelegationConfiguration configuration = new GroupDelegationConfiguration(true, true, "url", "", "", "", List.of());
		when(delGroupMan.getContents(project.path, project.path)).thenReturn(new DelegatedGroupContents(new DelegatedGroup("path", configuration, true, "name"), Optional.empty()));

		Group group = projectService.getProjectGroup(project);

		assertEquals(new Group("path", "name", true, true, "url", true, 0), group);
	}
}
