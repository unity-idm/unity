/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.members;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.UpManUI;
import io.imunity.upman.members.GroupMemberEntry.Role;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.GroupComboBox;
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
	private Map<String, String> additionalProjectAttributes;
	private GroupsManagement groupMan;

	public GroupMembersComponent(UnityMessageSource msg, GroupsManagement groupMan,
			GroupMembersController controller, String project)
			throws ControllerException
	{
		this.msg = msg;
		this.controller = controller;
		this.groupMan = groupMan;
		this.project = project;
		this.additionalProjectAttributes = controller
				.getAdditionalAttributeTypesForGroup(project);

		setMargin(false);
		setSpacing(false);

		List<SingleActionHandler<GroupMemberEntry>> actions = new ArrayList<>();
		actions.add(getRemoveFromProjectAction());
		actions.add(getRemoveFromGroupAction());
		actions.add(getAddToGroupAction());
		actions.add(getAddManagerPrivilegesAction());
		actions.add(getRevokeManagerPrivilegesAction());
		groupMemebersGrid = new GroupMemebersGrid(msg, actions,
				additionalProjectAttributes);
		HamburgerMenu<GroupMemberEntry> hamburgerMenu = new HamburgerMenu<>();
		groupMemebersGrid.addSelectionListener(hamburgerMenu.getSelectionListener());
		hamburgerMenu.addActionHandlers(actions);
		HorizontalLayout menuBar = new HorizontalLayout();
		menuBar.setSpacing(false);
		// TODO remove space, add styles
		Label space = new Label();
		space.setWidth(9, Unit.PIXELS);
		menuBar.addComponents(space, hamburgerMenu);
		addComponents(menuBar, groupMemebersGrid);
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
		controller.removeFromGroup(project, items);
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
		controller.removeFromGroup(group, items);
	}

	private SingleActionHandler<GroupMemberEntry> getAddToGroupAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage("GroupMemberGrid.addToGroupAction"))
				.withIcon(Images.add.getResource())
				.multiTarget()
				.withHandler(this::showAddToGroupDialog).build();
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
		controller.addManagerPrivileges(group, items);
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
		controller.revokeManagerPrivileges(group, items);
	}

	public String getGroup()
	{
		return group;
	}

	public void setGroup(String group)
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
			groupMembers = controller.getGroupMembers(
					additionalProjectAttributes.keySet(), group);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
			return;
		}

		groupMemebersGrid.setValue(groupMembers);
	}

	private void showAddToGroupDialog(Set<GroupMemberEntry> selection)
	{

		new TargetGroupSelectionDialog(msg, UpManUI.getProjectGroup(),
				group -> controller.addToGroup(group, selection)).show();
	}

	private class TargetGroupSelectionDialog extends AbstractDialog
	{
		private Consumer<String> selectionConsumer;
		private GroupComboBox groupSelection;
		private String rootGroup;

		public TargetGroupSelectionDialog(UnityMessageSource msg, String rootGroup,
				Consumer<String> selectionConsumer)
		{
			super(msg, msg.getMessage("AddToGroupDialog.caption"));
			this.selectionConsumer = selectionConsumer;
			this.rootGroup = rootGroup;
			setSizeEm(30, 18);
		}

		@Override
		protected FormLayout getContents()
		{
			Label info = new Label(msg.getMessage("AddToGroupDialog.info"));
			info.setWidth(100, Unit.PERCENTAGE);
			groupSelection = new GroupComboBox(
					msg.getMessage("AddToGroupDialog.selectGroup"), groupMan);
			groupSelection.setInput(rootGroup, false);
			groupSelection.setWidth(100, Unit.PERCENTAGE);
			FormLayout main = new CompactFormLayout();
			main.addComponents(info, groupSelection);
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			selectionConsumer.accept(groupSelection.getValue());
			close();
		}
	}
}
