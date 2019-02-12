/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.groups;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class TestGroupsController
{
	@Mock
	private UnityMessageSource mockMsg;

	@Mock
	private DelegatedGroupManagement mockDelGroupMan;

	private GroupsController controller;

	@Before
	public void initController()
	{
		controller = new GroupsController(mockMsg, mockDelGroupMan);
	}

	@Test
	public void shouldForwardAddGroupToCoreManager() throws ControllerException, EngineException
	{
		I18nString name = new I18nString("name");
		controller.addGroup("/project", "/", new I18nString("name"), true);
		verify(mockDelGroupMan).addGroup(eq("/project"), eq("/"), eq(name), eq(true));
	}

	@Test
	public void shouldForwardRemoveGroupToCoreManager() throws ControllerException, EngineException
	{
		controller.deleteGroup("/project", "/project/A");
		verify(mockDelGroupMan).removeGroup(eq("/project"), eq("/project/A"));
	}

	@Test
	public void shouldForwardSetModeToCoreManager() throws ControllerException, EngineException
	{
		controller.setGroupAccessMode("/project", "/project/A", false);
		verify(mockDelGroupMan).setGroupAccessMode(eq("/project"), eq("/project/A"), eq(false));
	}

	@Test
	public void shouldForwardSetGroupNameToCoreManager() throws ControllerException, EngineException
	{
		I18nString name = new I18nString("name");
		controller.updateGroupName("/project", "/project/A", name);
		verify(mockDelGroupMan).setGroupDisplayedName(eq("/project"), eq("/project/A"), eq(name));
	}

	@Test
	public void shouldForwardGetGroupsToCoreManager() throws ControllerException, EngineException
	{
		controller.getGroupTree("/project", "/project/A");
		verify(mockDelGroupMan).getGroupAndSubgroups(eq("/project"), eq("/project/A"));
	}

}
