/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.groups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Groups controller
 * 
 * @author P.Piernik
 *
 */
@Component
public class GroupsController
{

	private DelegatedGroupManagement delGroupMan;
	private UnityMessageSource msg;

	@Autowired
	public GroupsController(UnityMessageSource msg, DelegatedGroupManagement delGroupMan)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
	}

	public Map<String, List<Group>> getGroupTree(String projectPath, String rootPath)
			throws ControllerException
	{
		Map<String, GroupContents> groupAndSubgroups;
		try
		{
			groupAndSubgroups = delGroupMan.getGroupAndSubgroups(projectPath, rootPath);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupsController.getGroupError"),
					e.getMessage(), e);
		}

		Map<String, List<Group>> groupTree = new HashMap<>();

		for (String group : groupAndSubgroups.keySet())
		{
			List<Group> subGr = new ArrayList<>();
			for (String sgr : groupAndSubgroups.get(group).getSubGroups())
			{
				subGr.add(groupAndSubgroups.get(sgr).getGroup());
			}
			groupTree.put(group, subGr);
		}

		groupTree.put(null, Arrays.asList(groupAndSubgroups.get(rootPath).getGroup()));

		return groupTree;
	}

	public void addGroup(String project, String parentPath, I18nString groupName,
			boolean isOpen) throws ControllerException
	{
		try
		{
			delGroupMan.addGroup(project, parentPath, groupName, isOpen);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupsController.addGroupError",
							groupName.getValue(msg)),
					e.getMessage(), e);
		}
	}

	public void deleteGroup(String projectPath, String groupPath) throws ControllerException
	{
		try
		{
			delGroupMan.removeGroup(projectPath, groupPath);
		} catch (Exception e)
		{

			throw new ControllerException(
					msg.getMessage("GroupsController.deleteGroupError"),
					e.getMessage(), e);
		}

	}

	public void setGroupAccessMode(String projectPath, String groupPath, boolean isOpen)
			throws ControllerException
	{
		try
		{

			delGroupMan.setGroupAccessMode(projectPath, groupPath, isOpen);

		} catch (Exception e)
		{
			e.printStackTrace();
			throw new ControllerException(
					msg.getMessage("GroupsController.updateGroupAccessError"),
					e.getMessage(), e);
		}
	}

	public void updateGroupName(String projectPath, String groupPath, I18nString groupName)
			throws ControllerException
	{

		try
		{

			delGroupMan.setGroupDisplayedName(projectPath, groupPath, groupName);

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupsController.updateGroupNameError"),
					e.getMessage(), e);
		}
	}
}
