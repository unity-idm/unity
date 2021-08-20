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

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.upman.common.ServerFaultException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupContents;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.SubprojectGroupDelegationConfiguration;
import pl.edu.icm.unity.types.I18nString;
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
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, GroupsController.class);

	private final DelegatedGroupManagement delGroupMan;
	private final MessageSource msg;

	@Autowired
	public GroupsController(MessageSource msg, DelegatedGroupManagement delGroupMan)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
	}

	public Map<String, List<DelegatedGroup>> getGroupTree(String projectPath, String rootPath)
			throws ControllerException
	{
		Map<String, DelegatedGroupContents> groupAndSubgroups;
		try
		{
			groupAndSubgroups = delGroupMan.getGroupAndSubgroups(projectPath, rootPath);
		} catch (Exception e)
		{
			log.warn("Can not get group " + projectPath, e);
			throw new ServerFaultException(msg);
		}

		Map<String, List<DelegatedGroup>> groupTree = new HashMap<>();

		for (String group : groupAndSubgroups.keySet())
		{
			List<DelegatedGroup> subGr = new ArrayList<>();
			for (String sgr : groupAndSubgroups.get(group).subGroups)
			{
				subGr.add(groupAndSubgroups.get(sgr).group);
			}
			groupTree.put(group, subGr);
		}
		if (!groupAndSubgroups.isEmpty() && groupAndSubgroups.get(rootPath) != null)
		{
			groupTree.put(null, Arrays.asList(groupAndSubgroups.get(rootPath).group));
		}

		return groupTree;
	}

	public String addGroup(String projectPath, String parentPath, GroupWithAccessMode group)
			throws ControllerException
	{
		try
		{
			return delGroupMan.addGroup(projectPath, parentPath, group.name, group.isOpen);
		} catch (Exception e)
		{
			log.warn("Can not add group " + parentPath, e);
			throw new ServerFaultException(msg);
		}
	}

	public void deleteGroup(String projectPath, String groupPath) throws ControllerException
	{
		try
		{
			delGroupMan.removeGroup(projectPath, groupPath);
		} catch (Exception e)
		{
			log.warn("Can not remove group " + groupPath, e);
			throw new ServerFaultException(msg);
		}
	}
	
	public void deleteSubProjectGroup(String projectPath, String groupPath) throws ControllerException
	{
		try
		{
			delGroupMan.removeProject(projectPath, groupPath);
		} catch (Exception e)
		{
			log.warn("Can not remove sub-project group " + groupPath, e);
			throw new ServerFaultException(msg);
		}
	}

	public void setGroupAccessMode(String projectPath, String groupPath, boolean isOpen) throws ControllerException
	{
		try
		{
			delGroupMan.setGroupAccessMode(projectPath, groupPath, isOpen);

		} catch (Exception e)
		{
			log.warn("Can not set group access mode for " + groupPath, e);

			if (!projectPath.equals(groupPath))
			{
				throw new ServerFaultException(msg);
			} else
			{
				throw new ControllerException(
						msg.getMessage("GroupsController.projectGroupAccessModeChangeError"),
						msg.getMessage("GroupsController.projectGroupAccessModeChangeErrorDetails"),
						null);
			}
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
			log.warn("Can not rename group " + groupPath, e);
			throw new ServerFaultException(msg);
		}
	}

	public void setGroupDelegationConfiguration(String projectPath, String path,
			SubprojectGroupDelegationConfiguration groupDelegationConfig) throws ControllerException
	{
		try
		{

			delGroupMan.setGroupDelegationConfiguration(projectPath, path,
					groupDelegationConfig);

		} catch (Exception e)
		{
			log.warn("Can not set group delegation configuration in " + path, e);
			throw new ServerFaultException(msg);
		}

	}
}
