/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.members.GroupMemberEntry.Role;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * 
 * @author P.Piernik
 *
 */

public class GroupMembersComponent extends VerticalLayout
{

	private UnityMessageSource msg;
	private GroupMembersController controller;

	private GroupMemebersGrid groupMemebersGrid;
	private String group;
	private String project;
	private Collection<AttributeType> additionalProjectAttributes;

	public GroupMembersComponent(UnityMessageSource msg, GroupMembersController controller) throws ControllerException
	{
		this.msg = msg;
		this.controller = controller;
		this.additionalProjectAttributes = controller.getAdditionalAttributeTypesForGroup(group);
		
		setMargin(false);
		setSpacing(false);
		
		List<SingleActionHandler<GroupMemberEntry>> actions = new ArrayList<>();
		actions.add(getRemoveFromProjectAction());
		actions.add(getRemoveFromGroupAction());
		actions.add(getAddToGroupAction());
		actions.add(getAddManagerPrivilegesAction());
		actions.add(getRevokeManagerPrivilegesAction());
		groupMemebersGrid = new GroupMemebersGrid(msg, actions, additionalProjectAttributes);
		HamburgerMenu<GroupMemberEntry> hamburgerMenu = new HamburgerMenu<>();
		groupMemebersGrid.addSelectionListener(hamburgerMenu.getSelectionListener());
		hamburgerMenu.addActionHandlers(actions);
		addComponents(hamburgerMenu, groupMemebersGrid);
	}

	private SingleActionHandler<GroupMemberEntry> getRemoveFromProjectAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage(
						"GroupMemberGrid.removeFromProjectAction"))
				.withIcon(Images.removeFromGroup.getResource()).multiTarget()
				.withHandler(this::removeFromProject).build();
	}

	public void removeFromProject(Set<GroupMemberEntry> items)
	{

	}

	private SingleActionHandler<GroupMemberEntry> getRemoveFromGroupAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage(
						"GroupMemberGrid.removeFromGroupAction"))
				.withIcon(Images.deleteFolder.getResource()).multiTarget()
				.withHandler(this::removeFromProject).build();
	}

	public void removeFromGroup(Set<GroupMemberEntry> items)
	{

	}

	private SingleActionHandler<GroupMemberEntry> getAddToGroupAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage("GroupMemberGrid.addToGroupAction"))
				.withIcon(Images.add.getResource()).dontRequireTarget()
				.withHandler(this::addToGroup).build();
	}

	public void addToGroup(Set<GroupMemberEntry> items)
	{

	}

	private SingleActionHandler<GroupMemberEntry> getAddManagerPrivilegesAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage(
						"GroupMemberGrid.addManagerPrivilegesAction"))
				.withIcon(Images.trending_up.getResource()).multiTarget()
				.withHandler(this::addManagerPrivileges)
				.withDisabledPredicate(e -> !e.getRole().equals(Role.regular))
				.build();
	}

	public void addManagerPrivileges(Set<GroupMemberEntry> items)
	{

	}

	private SingleActionHandler<GroupMemberEntry> getRevokeManagerPrivilegesAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage(
						"GroupMemberGrid.revokeManagerPrivilegesAction"))
				.withIcon(Images.trending_down.getResource()).multiTarget()
				.withHandler(this::revokeManagerPrivileges)
				.withDisabledPredicate(e -> !e.getRole().equals(Role.admin))
				.build();
	}

	public void revokeManagerPrivileges(Set<GroupMemberEntry> items)
	{

	}

	public String getGroup()
	{
		return group;
	}

	public void setGroup(String project, String group)
	{
		this.group = group;
		reloadMemebersGrid();
	}

	private void reloadMemebersGrid()
	{
		groupMemebersGrid.clear();

		List<GroupMemberEntry> groupMembers;
		try
		{
			groupMembers = controller.getGroupMembers(additionalProjectAttributes, group);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}

		groupMemebersGrid.setValue(groupMembers);
	}
}
