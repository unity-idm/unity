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
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Groups controller
 * @author P.Piernik
 *
 */
@Component
public class GroupsController
{
	// TODO replace all by new management
	private BulkGroupQueryService groupQueryService;
	private GroupsManagement groupMan;

	private UnityMessageSource msg;

	@Autowired
	public GroupsController(UnityMessageSource msg, BulkGroupQueryService groupQueryService,
			GroupsManagement groupMan)
	{
		this.msg = msg;
		this.groupQueryService = groupQueryService;
		this.groupMan = groupMan;

	}

	public Map<String, List<Group>> getGroupTree(String root) throws ControllerException
	{
		GroupStructuralData bulkData;
		try
		{
			bulkData = groupQueryService.getBulkStructuralData(root);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupsController.getGroupError", root),
					e.getMessage(), e);
		}

		Map<String, GroupContents> groupAndSubgroups = groupQueryService
				.getGroupAndSubgroups(bulkData);

		Map<String, List<Group>> groupTree = new HashMap<>();

		for (String group : groupAndSubgroups.keySet())
		{
			List<Group> subGr = new ArrayList<>();
			for (String sgr : groupAndSubgroups.get(group).getSubGroups().stream()
					.sorted().collect(Collectors.toList()))
			{
				subGr.add(groupAndSubgroups.get(sgr).getGroup());
			}
			groupTree.put(group, subGr);
		}

		groupTree.put(null, Arrays.asList(groupAndSubgroups.get(root).getGroup()));

		return groupTree;
	}

	public void addGroup(Group group) throws ControllerException
	{
		try
		{
			groupMan.addGroup(group);
		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("GroupsController.addGroupError",
							group.getNameShort()),
					e.getMessage(), e);
		}
	}

	public void deleteGroup(String groupPath) throws ControllerException
	{
		try
		{
			groupMan.removeGroup(groupPath, true);
		} catch (EngineException e)
		{

			throw new ControllerException(
					msg.getMessage("GroupsController.deleteGroupError",
							new Group(groupPath).getNameShort()),
					e.getMessage(), e);
		}

	}

	public void setPrivateGroupAccess(String path) throws ControllerException
	{
		// TODO Auto-generated method stub

	}

	public void setPublicGroupAccess(String path) throws ControllerException
	{
		// TODO Auto-generated method stub

	}

	public void updateGroupName(String groupPath, I18nString groupName)
			throws ControllerException
	{

		GroupContents contents;
		try
		{
			contents = groupMan.getContents(groupPath, GroupContents.METADATA);

		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupsController.getGroupError",
							new Group(groupPath).getNameShort()),
					e.getMessage(), e);
		}

		Group group = contents.getGroup();
		group.setDisplayedName(groupName);

		try
		{

			groupMan.updateGroup(group.getName(), group);

		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("GroupsController.renameGroupError",
							group.getNameShort()),
					e.getMessage(), e);
		}

	}
}
