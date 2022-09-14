/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views;

import io.imunity.upman.front.model.Group;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.upman.utils.DelegatedGroupsHelper;
import io.imunity.upman.utils.ProjectService;
import io.imunity.vaadin23.elements.NotificationPresenter;
import io.imunity.vaadin23.endpoint.common.Vaddin23WebLogoutHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
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

	@BeforeEach
	public void initController()
	{
		projectService = new ProjectService(msg, delGroupMan, logoutHandler, delGroupHelper, notificationPresenter);
	}

	@Test
	public void shouldGetProjectForUser() throws EngineException
	{
		long entityId = 1;

		when(delGroupMan.getProjectsForEntity(entityId)).thenReturn(List.of(new DelegatedGroup("path", new GroupDelegationConfiguration(true), true, new I18nString("name"))));

		List<ProjectGroup> projectForUser = projectService.getProjectForUser(entityId);

		assertThat(List.of(new ProjectGroup("path", "name", "regForm", "singupForm"))).isEqualTo(projectForUser);
	}

	@Test
	public void shouldGetProjectLogo() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");

		GroupDelegationConfiguration configuration = new GroupDelegationConfiguration(true, true, "url", "", "", "", List.of());
		when(delGroupMan.getContents(project.path, project.path)).thenReturn(new DelegatedGroupContents(new DelegatedGroup("path", configuration, true, new I18nString("name")), Optional.empty()));

		String url = projectService.getProjectLogo(project);

		assertThat("url").isEqualTo(url);
	}

	@Test
	public void shouldGetProjectGroup() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");

		GroupDelegationConfiguration configuration = new GroupDelegationConfiguration(true, true, "url", "", "", "", List.of());
		when(delGroupMan.getContents(project.path, project.path)).thenReturn(new DelegatedGroupContents(new DelegatedGroup("path", configuration, true, new I18nString("name")), Optional.empty()));

		Group group = projectService.getProjectGroup(project);

		assertThat(new Group("path", new I18nString("name"), "name", true, true, "url", true, 0)).isEqualTo(group);
	}
}
