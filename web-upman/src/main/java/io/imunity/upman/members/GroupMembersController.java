/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.bulk.BulkGroupQueryService;
import pl.edu.icm.unity.engine.api.bulk.GroupStructuralData;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
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
	// TODO replace all by new management
	private GroupsManagement groupMan;
	private AttributeTypeManagement attrTypeMan;
	private AttributesManagement attrMan;
	private BulkGroupQueryService groupQueryService;

	private CachedAttributeHandlers cachedAttrHandlerRegistry;
	private UnityMessageSource msg;

	@Autowired
	public GroupMembersController(UnityMessageSource msg, GroupsManagement groupMan,
			AttributeTypeManagement attrTypeMan, AttributesManagement attrMan,
			AttributeHandlerRegistry attrHandlerRegistry,
			BulkGroupQueryService groupQueryService)
	{
		this.msg = msg;
		this.groupMan = groupMan;
		this.attrTypeMan = attrTypeMan;
		this.attrMan = attrMan;
		this.groupQueryService = groupQueryService;
		this.cachedAttrHandlerRegistry = new CachedAttributeHandlers(attrHandlerRegistry);
	}

	// TODO new manager should replace DelegatedGroupMemebership with email,
	// name and role
	public List<GroupMemberEntry> getGroupMembers(Collection<String> additionalAttributeNames,
			String groupPath) throws ControllerException
	{

		List<GroupMemberEntry> ret = new ArrayList<>();

		GroupContents contents = null;
		try
		{
			contents = groupMan.getContents(groupPath, GroupContents.MEMBERS);
		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("GroupMembersController.getGroupError",
							new Group(groupPath).getNameShort()),
					e.getMessage(), e);
		}

		for (GroupMembership member : contents.getMembers())
		{
			GroupMemberEntry entry = new GroupMemberEntry(member.getEntityId(),
					member.getGroup(),
					getMemberAttribute(member, additionalAttributeNames,
							groupPath),
					(member.getEntityId() & 1) == 0
							? GroupMemberEntry.Role.regular
							: GroupMemberEntry.Role.admin,
					"name" + member.getEntityId(),
					"email" + member.getEntityId() + "@imunity.io");
			ret.add(entry);
		}

		return ret;
	}

	// TODO move this to new manager
	private Map<String, String> getMemberAttribute(GroupMembership member,
			Collection<String> attributes, String groupPath) throws ControllerException
	{

		Map<String, String> attributesVal = new HashMap<>();
		for (String atype : attributes)
		{
			Collection<AttributeExt> attrs = null;
			try
			{
				attrs = attrMan.getAttributes(new EntityParam(member.getEntityId()),
						groupPath, atype);
			} catch (EngineException e)
			{
				throw new ControllerException(msg.getMessage(
						"GroupMembersController.getAttributesError",
						member.getEntityId(),
						new Group(groupPath).getNameShort()),
						e.getMessage(), e);
			}

			for (AttributeExt a : attrs)
			{

				attributesVal.put(atype, cachedAttrHandlerRegistry
						.getSimplifiedAttributeValuesRepresentation(a));
			}
		}

		return attributesVal;

	}

	public Map<String, String> getGroupsMap(String rootPath) throws ControllerException
	{
		Map<String, String> groups = new HashMap<>();
		GroupStructuralData bulkData;
		try
		{
			bulkData = groupQueryService.getBulkStructuralData(rootPath);
		} catch (Exception e)
		{
			throw new ControllerException(
					msg.getMessage("GroupMembersController.getGroupError",
							rootPath),
					e.getMessage(), e);
		}

		Map<String, GroupContents> groupAndSubgroups = groupQueryService
				.getGroupAndSubgroups(bulkData);

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

	// TODO replace subgroup string |-
	private String generateIndent(int count)
	{
		return String.join("", Collections.nCopies(count, " ")) + "|-";
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
	public Map<String, String> getAdditionalAttributeTypesForGroup(String groupPath)
			throws ControllerException
	{
		Map<String, String> ret = new HashMap<>();
		try
		{
			AttributeType attr = null;
			if (groupPath.length() == 2)
				attr = attrTypeMan.getAttributeType("mobile");
			else
				attr = attrTypeMan.getAttributeType("firstname");

			ret.put(attr.getName(), attr.getDisplayedName().getValue(msg));
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage(
					"GroupMembersController.getGroupAttributeError"),
					e.getMessage(), e);
		}
		return ret;
	}

	public void addToGroup(String groupPath, Set<GroupMemberEntry> selection)
	{
		// TODO Auto-generated method stub
	}

	public void removeFromGroup(String groupPath, Set<GroupMemberEntry> selection)
	{
		// TODO Auto-generated method stub
	}

	public void addManagerPrivileges(String groupPath, Set<GroupMemberEntry> items)
	{
		// TODO Auto-generated method stub

	}

	public void revokeManagerPrivileges(String groupPath, Set<GroupMemberEntry> items)
	{
		// TODO Auto-generated method stub

	}
}
