/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.groups;

import io.imunity.upman.av23.front.components.NotificationPresenter;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.ProjectGroup;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;

import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TestGroupsService
{
	@Mock
	private MessageSource mockMsg;
	@Mock
	private DelegatedGroupManagement mockDelGroupMan;
	@Mock
	private NotificationPresenter notificationPresenter;

	private GroupService groupService;

	@Before
	public void initController()
	{
		groupService = new GroupService(mockMsg, mockDelGroupMan, notificationPresenter);
	}

	@Test
	public void shouldForwardAddGroupToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		Group group = new Group("/", "group", false, false, "", false, 0);
		Map<Locale, String> translations = Map.of(Locale.UK, "uk");
		I18nString names = new I18nString(Locale.UK.getLanguage(), "uk");

		groupService.addGroup(project, group, translations, true);

		verify(mockDelGroupMan).addGroup(eq("/project"), eq("/"), eq(names), eq(true));
	}

	@Test
	public void shouldForwardRemoveGroupToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		Group group = new Group("/project/A", "group", false, false, "", false, 0);

		groupService.deleteGroup(project, group);

		verify(mockDelGroupMan).removeGroup(eq("/project"), eq("/project/A"));
	}

	@Test
	public void shouldForwardRemoveProjectToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		Group group = new Group("/project/A", "group", false, false, "", false, 0);

		groupService.deleteSubProjectGroup(project, group);

		verify(mockDelGroupMan).removeProject(eq("/project"), eq("/project/A"));
	}

	@Test
	public void shouldForwardSetModeToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		Group group = new Group("/project/A", "group", false, false, "", false, 0);

		groupService.setGroupAccessMode(project, group, false);

		verify(mockDelGroupMan).setGroupAccessMode(eq("/project"), eq("/project/A"), eq(false));
	}

	@Test
	public void shouldForwardSetGroupNameToCoreManager() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project");
		Group group = new Group("/project/A", "group", false, false, "", false, 0);
		Map<Locale, String> translations = Map.of(Locale.UK, "uk");
		I18nString names = new I18nString(Locale.UK.getLanguage(), "uk");

		groupService.updateGroupName(project, group, translations);

		verify(mockDelGroupMan).setGroupDisplayedName(eq("/project"), eq("/project/A"), eq(names));
	}
}
