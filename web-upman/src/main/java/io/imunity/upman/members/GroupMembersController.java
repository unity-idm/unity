/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.AttributesManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.GroupContents;
import pl.edu.icm.unity.types.basic.GroupMembership;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.CachedAttributeHandlers;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class GroupMembersController
{
	//TODO replace all by new management
	private GroupsManagement groupMan;
	private AttributeTypeManagement attrTypeMan;
	private AttributesManagement attrMan;
	private CachedAttributeHandlers cachedAttrHandlerRegistry;
	private UnityMessageSource msg;

	@Autowired
	public GroupMembersController(UnityMessageSource msg, GroupsManagement groupMan,
			AttributeTypeManagement attrTypeMan, AttributesManagement attrMan,
			AttributeHandlerRegistry attrHandlerRegistry)
	{
		this.msg = msg;
		this.groupMan = groupMan;
		this.attrTypeMan = attrTypeMan;
		this.attrMan = attrMan;
		this.cachedAttrHandlerRegistry = new CachedAttributeHandlers(attrHandlerRegistry);
	}

	// TODO
	public List<GroupMemberEntry> getGroupMembers(Collection<String> additionalAttributeNames,
			String group) throws ControllerException
	{

		List<GroupMemberEntry> ret = new ArrayList<>();

		GroupContents contents = null;
		try
		{
			contents = groupMan.getContents(group, GroupContents.MEMBERS);
		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage(
					"GroupMembersController.getGroupContentError", group),
					e.getMessage(), e);
		}

		for (GroupMembership member : contents.getMembers())
		{
			GroupMemberEntry entry = new GroupMemberEntry(member.getEntityId(),
					member.getGroup(),
					getMemberAttribute(member, additionalAttributeNames, group),
					(member.getEntityId() & 1) == 0
							? GroupMemberEntry.Role.regular
							: GroupMemberEntry.Role.admin,
					"name" + member.getEntityId(),
					"email" + member.getEntityId() + "@imunity.io");
			ret.add(entry);
		}

		return ret;
	}

	private Map<String, String> getMemberAttribute(GroupMembership member,
			Collection<String> attributes, String group) throws ControllerException
	{

		Map<String, String> attributesVal = new HashMap<>();
		for (String atype : attributes)
		{
			Collection<AttributeExt> attrs = null;
			try
			{
				attrs = attrMan.getAttributes(new EntityParam(member.getEntityId()),
						group, atype);
			} catch (EngineException e)
			{
				throw new ControllerException(msg.getMessage(
						"GroupMembersController.getAttributesError",
						member.getEntityId(), group), e.getMessage(), e);
			}

			for (AttributeExt a : attrs)
			{

				attributesVal.put(atype, cachedAttrHandlerRegistry
						.getSimplifiedAttributeValuesRepresentation(a));
			}
		}

		return attributesVal;

	}

	public Map<String, String> getGroupsMap(String root) throws ControllerException
	{
		Map<String, String> groups = new HashMap<>();

		Set<String> childGroups = null;
		try
		{
			childGroups = groupMan.getChildGroups(root);
		} catch (EngineException e)
		{
			throw new ControllerException(
					msg.getMessage("GroupMembersController.getChildGroupsError",
							root),
					e.getMessage(), e);
		}

		for (String childGroup : childGroups)
		{
			groups.put(childGroup,
					"|_" + childGroup.replace(root, "").replace("/", "_"));
		}

		groups.put(root, root.substring(root.lastIndexOf('/') + 1) + " ("
				+ msg.getMessage("AllMemebers") + ")");

		return groups;

	}

	// TODO get attr based on group del config
	public Map<String, String> getAdditionalAttributeTypesForGroup(String group)
			throws ControllerException
	{
		Map<String, String> ret = new HashMap<>();
		try
		{
			AttributeType attr = null;
			if (group.length() == 2)
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

	public void addToGroup(String group, Set<GroupMemberEntry> selection)
	{
		// TODO Auto-generated method stub
	}

	public void removeFromGroup(String group, Set<GroupMemberEntry> selection)
	{
		// TODO Auto-generated method stub
	}

	public void addManagerPrivileges(String group, Set<GroupMemberEntry> items)
	{
		// TODO Auto-generated method stub

	}

	public void revokeManagerPrivileges(String group, Set<GroupMemberEntry> items)
	{
		// TODO Auto-generated method stub

	}
}
