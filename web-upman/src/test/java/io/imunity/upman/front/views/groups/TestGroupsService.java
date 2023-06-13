/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.groups;

import io.imunity.upman.front.model.Group;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;

import java.util.Locale;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TestGroupsService
{
	@Mock
	private MessageSource mockMsg;
	@Mock
	private DelegatedGroupManagement mockDelGroupMan;
	@Mock
	private NotificationPresenter notificationPresenter;

	private GroupService groupService;

	@BeforeEach
	public void initController()
	{
		groupService = new GroupService(mockMsg, mockDelGroupMan, notificationPresenter);
	}

	@Test
	public void shouldAddGroup() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		Group group = new Group("/", new I18nString("group"), "group", false, false, "", false, 0);
		Map<Locale, String> translations = Map.of(Locale.UK, "uk");
		I18nString names = new I18nString(Locale.UK.getLanguage(), "uk");

		groupService.addGroup(project, group, translations, true);

		verify(mockDelGroupMan).addGroup(eq("/project"), eq("/"), eq(names), eq(true));
	}

	@Test
	public void shouldRemoveGroup() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		Group group = new Group("/project/A", new I18nString("group"), "group", false, false, "", false, 0);

		groupService.deleteGroup(project, group);

		verify(mockDelGroupMan).removeGroup(eq("/project"), eq("/project/A"));
	}

	@Test
	public void shouldRemoveProject() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		Group group = new Group("/project/A", new I18nString("group"), "group", false, false, "", false, 0);

		groupService.deleteSubProjectGroup(project, group);

		verify(mockDelGroupMan).removeProject(eq("/project"), eq("/project/A"));
	}

	@Test
	public void shouldSetMode() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		Group group = new Group("/project/A", new I18nString("group"), "group", false, false, "", false, 0);

		groupService.setGroupAccessMode(project, group, false);

		verify(mockDelGroupMan).setGroupAccessMode(eq("/project"), eq("/project/A"), eq(false));
	}

	@Test
	public void shouldSetGroupName() throws EngineException
	{
		ProjectGroup project = new ProjectGroup("/project", "project", "regForm", "singupForm");
		Group group = new Group("/project/A", new I18nString("group"), "group", false, false, "", false, 0);
		Map<Locale, String> translations = Map.of(Locale.UK, "uk");
		I18nString names = new I18nString(Locale.UK.getLanguage(), "uk");

		groupService.updateGroupName(project, group, translations);

		verify(mockDelGroupMan).setGroupDisplayedName(eq("/project"), eq("/project/A"), eq(names));
	}
}
