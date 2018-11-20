/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pl.edu.icm.unity.engine.api.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DelegatedGroupMember;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupAuthorizationRole;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.CachedAttributeHandlers;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Group members controller
 * 
 * @author P.Piernik
 *
 */
@Component
public class GroupMembersController
{

	private DelegatedGroupManagement delGroupMan;
	private CachedAttributeHandlers cachedAttrHandlerRegistry;
	private UnityMessageSource msg;

	@Autowired
	public GroupMembersController(UnityMessageSource msg, GroupsManagement groupMan,
			AttributeHandlerRegistry attrHandlerRegistry,
			DelegatedGroupManagement delGroupMan)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
		this.cachedAttrHandlerRegistry = new CachedAttributeHandlers(attrHandlerRegistry);
	}

	public List<GroupMemberEntry> getGroupMembers(String projectPath, String groupPath)
			throws ControllerException
	{

		List<GroupMemberEntry> ret = new ArrayList<>();

		List<DelegatedGroupMember> contents = null;
		try
		{
			contents = delGroupMan.getGroupMembers(projectPath, groupPath);
		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("GroupMembersController.getGroupError",
							new Group(groupPath).getNameShort()),
					e.getMessage(), e);
		}

		for (DelegatedGroupMember member : contents)
		{

			Map<String, String> additionalAttributes = new HashMap<>();

			for (Attribute attr : member.attributes)
			{
				additionalAttributes.put(attr.getName(), cachedAttrHandlerRegistry
						.getSimplifiedAttributeValuesRepresentation(attr));
			}

			GroupMemberEntry entry = new GroupMemberEntry(member, additionalAttributes);
			ret.add(entry);
		}

		return ret;
	}

	public Map<String, String> getProjectGroupsMap(String rootPath) throws ControllerException
	{
		Map<String, String> groups = new HashMap<>();
		Map<String, GroupContents> groupAndSubgroups;
		try
		{
			groupAndSubgroups = delGroupMan.getGroupAndSubgroups(rootPath, rootPath);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupMembersController.getGroupError",
							rootPath),
					e.getMessage(), e);
		}

		groups.put(rootPath,
				getGroupDisplayName(groupAndSubgroups.get(rootPath).getGroup()));

		fillGroupRecursive(rootPath, groupAndSubgroups, groups);

		return getGroupTree(rootPath, groups);
	}

	private String getGroupDisplayName(Group group)
	{
		String displayName = group.getDisplayedName().getValue(msg);

		if (group.getName().equals(displayName))
		{
			return group.getNameShort();
		}

		return displayName;
	}

	private Map<String, String> getGroupTree(String rootPath, Map<String, String> groups)
	{
		Map<String, String> tree = new HashMap<>();

		int initIndend = StringUtils.countOccurrencesOf(rootPath, "/");

		tree.put(rootPath, groups.get(rootPath));
		for (String gr : groups.keySet().stream().filter(i -> !i.equals(rootPath))
				.collect(Collectors.toList()))
		{
			tree.put(gr, generateIndent(
					StringUtils.countOccurrencesOf(gr, "/") - initIndend)
					+ groups.get(gr));
		}
		return tree;

	}

	private String generateIndent(int count)
	{
		return String.join("", Collections.nCopies(count, "\u2003"));
	}

	private void fillGroupRecursive(String parentPath,
			Map<String, GroupContents> groupAndSubgroups, Map<String, String> groups)
	{
		for (String subgroup : groupAndSubgroups.get(parentPath).getSubGroups())
		{
			groups.put(subgroup, getGroupDisplayName(
					groupAndSubgroups.get(subgroup).getGroup()));
			fillGroupRecursive(subgroup, groupAndSubgroups, groups);
		}

	}

	// TODO get attr based on group del config
	public Map<String, String> getAdditionalAttributeNamesForProject(String groupPath)
			throws ControllerException
	{
		try
		{
			return delGroupMan.getAdditionalAttributeNamesForProject(groupPath);
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage(
					"GroupMembersController.getGroupAttributeError"),
					e.getMessage(), e);
		}
	}

	public void addToGroup(String projectPath, String groupPath, Set<GroupMemberEntry> items)
			throws ControllerException
	{
		try
		{
			for (GroupMemberEntry member : items)
			{
				delGroupMan.addMemberToGroup(projectPath, groupPath,
						member.getEntityId());
			}
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage(
					"GroupMembersController.addToGroupError", groupPath),
					e.getMessage(), e);
		}
	}

	public void removeFromGroup(String projectPath, String groupPath,
			Set<GroupMemberEntry> items) throws ControllerException
	{
		try
		{
			for (GroupMemberEntry member : items)
			{
				delGroupMan.removeMemberFromGroup(projectPath, groupPath,
						member.getEntityId());

			}
		} catch (EngineException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void addManagerPrivileges(String groupPath, Set<GroupMemberEntry> items)
			throws ControllerException
	{
		try
		{
			for (GroupMemberEntry member : items)
			{
				delGroupMan.setGroupAuthorizationRole(groupPath,
						member.getEntityId(),
						GroupAuthorizationRole.manager);
			}
		} catch (EngineException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// TODO check if one admin is in group
	public void revokeManagerPrivileges(String groupPath, Set<GroupMemberEntry> items)
			throws ControllerException
	{
		try
		{
			for (GroupMemberEntry member : items)
			{
				delGroupMan.setGroupAuthorizationRole(groupPath,
						member.getEntityId(),
						GroupAuthorizationRole.regular);
			}
		} catch (EngineException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
