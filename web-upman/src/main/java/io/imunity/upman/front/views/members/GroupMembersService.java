/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.front.views.members;

import io.imunity.upman.front.model.Group;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.attributes.CachedAttributeHandlers;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupManagement;
import pl.edu.icm.unity.engine.api.project.DelegatedGroupMember;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;

import java.util.*;


@Service
class GroupMembersService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_UPMAN, GroupMembersService.class);

	private final DelegatedGroupManagement delGroupMan;
	private final CachedAttributeHandlers cachedAttrHandlerRegistry;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;

	public GroupMembersService(MessageSource msg,
	                           AttributeHandlerRegistry attrHandlerRegistry, DelegatedGroupManagement delGroupMan,
	                           NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.delGroupMan = delGroupMan;
		this.cachedAttrHandlerRegistry = new CachedAttributeHandlers(attrHandlerRegistry);
		this.notificationPresenter = notificationPresenter;
	}

	public List<MemberModel> getGroupMembers(ProjectGroup projectGroup, Group group)
	{

		List<MemberModel> ret = new ArrayList<>();

		List<DelegatedGroupMember> members = null;
		try
		{
			members = delGroupMan.getDelegatedGroupMembers(projectGroup.path, group.path);
		} catch (Exception e)
		{
			log.warn("Can not get memebers of group " + projectGroup.path, e);
			notificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
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
			notificationPresenter.showError(msg.getMessage("ServerFaultExceptionCaption"), msg.getMessage("ContactSupport"));
		}
		return attrs;
	}

	public void addToGroup(ProjectGroup projectGroup, List<Group> groups, Set<MemberModel> items)
	{
		List<String> added = new ArrayList<>();

		try
		{
			for (MemberModel member : items)
			{
				for (Group group : groups)
				{
					delGroupMan.addMemberToGroup(projectGroup.path, group.path, member.entityId);
				}
				added.add(member.name);
			}
			notificationPresenter.showSuccess(msg.getMessage("GroupMembersComponent.addedToGroup"));
		} catch (Exception e)
		{
			log.warn("Can not add member to group " + groups, e);
			if (added.isEmpty())
			{
				notificationPresenter.showError(msg.getMessage("GroupMembersController.addToGroupError"), msg.getMessage("GroupMembersController.notAdded"));
			} else
			{
				notificationPresenter.showError(msg.getMessage("GroupMembersController.addToGroupError"), msg.getMessage("GroupMembersController.partiallyAdded", added));
			}
		}
	}

	public void removeFromProject(ProjectGroup projectGroup, Set<MemberModel> items)
	{
		extracted(items, projectGroup, projectGroup.path);
	}

	public void removeFromGroup(ProjectGroup projectGroup, Group group, Set<MemberModel> items)
	{
		extracted(items, projectGroup, group.path);
	}

	private void extracted(Set<MemberModel> items, ProjectGroup projectGroup, String groupPath)
	{
		List<String> removed = new ArrayList<>();

		try
		{
			for (MemberModel member : items)
			{
				delGroupMan.removeMemberFromGroup(projectGroup.path, groupPath, member.entityId);
				removed.add(member.name);
			}
			notificationPresenter.showSuccess(msg.getMessage("GroupMembersComponent.removed"));
		} catch (Exception e)
		{
			log.warn("Can not remove member from group " + groupPath, e);
			if (removed.isEmpty())
			{
				notificationPresenter.showError(msg.getMessage("GroupMembersController.removeFromGroupError"), msg.getMessage("GroupMembersController.notRemoved"));
			} else
			{
				notificationPresenter.showError(msg.getMessage("GroupMembersController.removeFromGroupError"), msg.getMessage("GroupMembersController.partiallyRemoved", removed));
			}
		}
	}

	public void updateRole(ProjectGroup projectGroup, Group group, GroupAuthorizationRole role, Set<MemberModel> items)
	{
		List<String> updated = new ArrayList<>();

		try
		{
			for (MemberModel member : items)
			{
				delGroupMan.setGroupAuthorizationRole(projectGroup.path, group.path, member.entityId, role);
				updated.add(member.name);
			}
			notificationPresenter.showSuccess(msg.getMessage("GroupMembersComponent.role.updated"));
		} catch (Exception e)
		{
			log.warn("Can not update group authorization role", e);
			if (updated.isEmpty())
			{
				notificationPresenter.showError(msg.getMessage("GroupMembersController.updatePrivilegesError"), msg.getMessage("GroupMembersController.notUpdated"));
			} else
			{
				notificationPresenter.showError(msg.getMessage("GroupMembersController.updatePrivilegesError"), msg.getMessage("GroupMembersController.partiallyUpdated", updated));
			}
		}
	}

}
