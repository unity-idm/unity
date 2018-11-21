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

	public Map<String, List<Group>> getGroupTree(String project, String root)
			throws ControllerException
	{
		Map<String, GroupContents> groupAndSubgroups;
		try
		{
			groupAndSubgroups = delGroupMan.getGroupAndSubgroups(project, root);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupsController.getGroupError", root),
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

		groupTree.put(null, Arrays.asList(groupAndSubgroups.get(root).getGroup()));

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

	public void deleteGroup(String project, String groupPath) throws ControllerException
	{
		try
		{
			delGroupMan.removeGroup(project, groupPath, true);
		} catch (Exception e)
		{

			throw new ControllerException(
					msg.getMessage("GroupsController.deleteGroupError",
							new Group(groupPath).getNameShort()),
					e.getMessage(), e);
		}

	}

	public void setGroupAccessMode(String project, String path, boolean isOpen)
			throws ControllerException
	{
		try
		{

			delGroupMan.setGroupAccessMode(project, path, isOpen);

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupsController.updateGroupAccessError",
							new Group(path).getNameShort()),
					e.getMessage(), e);
		}

	}

	public void updateGroupName(String project, String path, I18nString groupName)
			throws ControllerException
	{

		try
		{

			delGroupMan.setGroupDisplayedName(project, path, groupName);

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupsController.updateGroupNameError",
							new Group(path).getNameShort()),
					e.getMessage(), e);
		}
	}
}
