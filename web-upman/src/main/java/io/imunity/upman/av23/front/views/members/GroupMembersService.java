/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.front.views.members;

import io.imunity.upman.av23.front.components.NotificationPresenter;
import io.imunity.upman.av23.front.model.Group;
import io.imunity.upman.av23.front.model.ProjectGroup;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.CachedAttributeHandlers;

import java.util.*;


@Service
class GroupMembersService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, GroupMembersService.class);

	private final DelegatedGroupManagement delGroupMan;
	private final CachedAttributeHandlers cachedAttrHandlerRegistry;
	private final MessageSource msg;

	public GroupMembersService(MessageSource msg,
	                           AttributeHandlerRegistry attrHandlerRegistry, DelegatedGroupManagement delGroupMan)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
		this.cachedAttrHandlerRegistry = new CachedAttributeHandlers(attrHandlerRegistry);
	}

	public List<MemberModel> getGroupMembers(ProjectGroup projectGroup, Group group)
	{

		List<MemberModel> ret = new ArrayList<>();

		List<DelegatedGroupMember> members = null;
		try
		{
			members = delGroupMan.getDelegatedGroupMemebers(projectGroup.path, group.path);
		} catch (Exception e)
		{
			log.warn("Can not get memebers of group " + projectGroup.path, e);
			NotificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}

		if (members == null || members.isEmpty())
			return ret;

		for (DelegatedGroupMember member : members)
		{
			Map<String, String> additionalAttributes = new HashMap<>();

			for (Attribute attr : member.attributes)
			{
				additionalAttributes.put(attr.getName(), cachedAttrHandlerRegistry
						.getSimplifiedAttributeValuesRepresentation(attr));
			}

			MemberModel model = MemberModel.builder()
					.entityId(member.entityId)
					.role(member.role)
					.name(member.name)
					.attributes(additionalAttributes)
					.email(member.email)
					.build();
			ret.add(model);
		}

		return ret;
	}

	public Map<String, String> getAdditionalAttributeNamesForProject(ProjectGroup projectGroup)
	{

		Map<String, String> attrs = new LinkedHashMap<>();
		try
		{
			DelegatedGroup group = delGroupMan.getContents(projectGroup.path, projectGroup.path).group;
			if (group == null)
				return attrs;

			List<String> groupAttrs = group.delegationConfiguration.attributes;

			if (groupAttrs == null || groupAttrs.isEmpty())
				return attrs;

			for (String attr : groupAttrs)
			{
				attrs.put(attr, delGroupMan.getAttributeDisplayedName(projectGroup.path, attr));
			}
		} catch (Exception e)
		{
			log.warn("Can not get attribute names for project " + projectGroup.path, e);
			NotificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
		return attrs;
	}

	public void addToGroup(String projectPath, String groupPath, Set<MemberModel> items)
	{
		List<String> added = new ArrayList<>();

		try
		{
			for (MemberModel member : items)
			{
				delGroupMan.addMemberToGroup(projectPath, groupPath, member.entityId);
				added.add(member.name);
			}
			NotificationPresenter.showSuccess(msg.getMessage("GroupMembersComponent.addedToGroup"));
		} catch (Exception e)
		{
			log.warn("Can not add member to group " + groupPath, e);
			if (added.isEmpty())
			{
				NotificationPresenter.showError(msg.getMessage("GroupMembersController.addToGroupError"), msg.getMessage("GroupMembersController.notAdded"));
			} else
			{
				NotificationPresenter.showError(msg.getMessage("GroupMembersController.addToGroupError"), msg.getMessage("GroupMembersController.partiallyAdded", added));
			}
		}
	}

	public void removeFromGroup(String projectPath, String groupPath, Set<MemberModel> items)
	{
		List<String> removed = new ArrayList<>();

		try
		{
			for (MemberModel member : items)
			{
				delGroupMan.removeMemberFromGroup(projectPath, groupPath, member.entityId);
				removed.add(member.name);
			}
			NotificationPresenter.showSuccess(msg.getMessage("GroupMembersComponent.removed"));
		} catch (Exception e)
		{
			log.warn("Can not remove member from group " + groupPath, e);
			if (removed.isEmpty())
			{
				NotificationPresenter.showError(msg.getMessage("GroupMembersController.removeFromGroupError"), msg.getMessage("GroupMembersController.notRemoved"));
			} else
			{
				NotificationPresenter.showError(msg.getMessage("GroupMembersController.removeFromGroupError"), msg.getMessage("GroupMembersController.partiallyRemoved", removed));
			}
		}
	}

	public void updateRole(String projectPath, String groupPath, GroupAuthorizationRole role, Set<MemberModel> items)
	{

		List<String> updated = new ArrayList<>();

		try
		{
			for (MemberModel member : items)
			{
				delGroupMan.setGroupAuthorizationRole(projectPath, groupPath, member.entityId, role);
				updated.add(member.name);
			}
			NotificationPresenter.showSuccess(msg.getMessage("GroupMembersComponent.role.updated"));
		} catch (Exception e)
		{
			log.warn("Can not update group authorization role", e);
			if (updated.isEmpty())
			{
				NotificationPresenter.showError(msg.getMessage("GroupMembersController.updatePrivilegesError"), msg.getMessage("GroupMembersController.notUpdated"));
			} else
			{
				NotificationPresenter.showError(msg.getMessage("GroupMembersController.updatePrivilegesError"), msg.getMessage("GroupMembersController.partiallyUpdated", updated));
			}
		}
	}

}
