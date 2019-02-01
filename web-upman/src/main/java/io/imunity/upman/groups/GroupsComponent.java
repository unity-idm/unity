/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component displays groups in tree grid with hamburger menu on the top
 * @author P.Piernik
 *
 */

class GroupsComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private GroupsController controller;
	private GroupsTree groupBrowser;
	private String projectPath;

	public GroupsComponent(UnityMessageSource msg, GroupsController controller,
			String projectPath) throws ControllerException
	{
		this.msg = msg;
		this.controller = controller;
		this.projectPath = projectPath;

		List<SingleActionHandler<GroupNode>> rawActions = new ArrayList<>();
		rawActions.add(getDeleteGroupAction());
		rawActions.add(getAddToGroupAction());
		rawActions.add(getMakePublicAction());
		rawActions.add(getMakePrivateAction());
		rawActions.add(getRenameGroupcAction());

		groupBrowser = new GroupsTree(msg, controller, rawActions, projectPath);
		HamburgerMenu<GroupNode> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addStyleNames(SidebarStyles.indentSmall.toString());
		hamburgerMenu.addStyleName(SidebarStyles.sidebar.toString());
		
		groupBrowser.addSelectionListener(hamburgerMenu.getSelectionListener());

		hamburgerMenu.addActionHandler(getExpandAllAction());
		hamburgerMenu.addActionHandler(getCollapseAllAction());

		HorizontalLayout menuBar = new HorizontalLayout(hamburgerMenu);
		VerticalLayout vl = new VerticalLayout();
		vl.setMargin(false);
		vl.setSpacing(false);
		vl.addComponents(menuBar, groupBrowser);
		setCompositionRoot(vl);
	}

	private SingleActionHandler<GroupNode> getMakePrivateAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.makePrivateAction"))
				.withIcon(Images.padlock_lock.getResource())
				.withDisabledPredicate(n -> {
					boolean disabled = !n.isOpen();
					for (GroupNode child : groupBrowser.getChildren(n))
					{
						disabled = disabled || child.isOpen();
					}
					return disabled;
				}).withHandler(this::makePrivate).hideIfInactive().build();
	}

	private void makePrivate(Set<GroupNode> items)
	{

		updateGroupAccess(items, false);
	}

	private SingleActionHandler<GroupNode> getMakePublicAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.makePublicAction"))
				.withIcon(Images.padlock_unlock.getResource())
				.withDisabledPredicate(n -> {
					boolean disabled = n.isOpen();
					if (n.getParentNode() != null)
					{
						disabled = disabled || !n.getParentNode().isOpen();
					}
					return disabled;
				})

				.withHandler(this::makePublic).hideIfInactive().build();
	}

	private void makePublic(Set<GroupNode> items)
	{
		updateGroupAccess(items, true);
	}
	
	private void updateGroupAccess(Set<GroupNode> items, boolean isPublic)
	{
		if (items.isEmpty())
			return;
		GroupNode groupNode = items.iterator().next();

		try
		{

			controller.setGroupAccessMode(projectPath, groupNode.getPath(), isPublic);
			groupBrowser.reloadNode(groupNode);

		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private SingleActionHandler<GroupNode> getExpandAllAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.expandAllAction"))
				.withIcon(Images.file_tree_sub.getResource()).dontRequireTarget()
				.withHandler(e -> groupBrowser.expandAll()).build();
	}

	private SingleActionHandler<GroupNode> getCollapseAllAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.collapseAllAction"))
				.withIcon(Images.file_tree_small.getResource()).dontRequireTarget().withHandler(e -> {
					groupBrowser.collapseAll();
					groupBrowser.expandRoot();
				}).build();
	}

	private SingleActionHandler<GroupNode> getDeleteGroupAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.deleteGroupAction"))
				.withIcon(Images.removeFromGroup.getResource())
				.withDisabledPredicate(n -> n.getPath().equals(projectPath))
				.hideIfInactive().withHandler(this::confirmDelete).build();
	}

	private void confirmDelete(Set<GroupNode> items)
	{

		if (items.isEmpty())
			return;

		GroupNode groupNode = items.iterator().next();
		new ConfirmDialog(msg, msg.getMessage("RemoveGroupDialog.confirmDelete",
				groupNode.toString()), () -> deleteGroup(groupNode)

		).show();
	}

	private void deleteGroup(GroupNode group)
	{
		try
		{

			controller.deleteGroup(projectPath, group.getPath());
			groupBrowser.reloadNode(group.getParentNode());

		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}

	}

	private SingleActionHandler<GroupNode> getAddToGroupAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.addGroupAction"))
				.withIcon(Images.add.getResource()).multiTarget()
				.withHandler(this::showAddGroupDialog).build();
	}

	private void showAddGroupDialog(Set<GroupNode> items)
	{

		if (items.isEmpty())
			return;

		GroupNode groupNode = items.iterator().next();
		new AddGroupDialog(msg, groupNode, (groupName, isOpen) -> {
			try
			{
				controller.addGroup(projectPath, groupNode.getPath(), groupName,
						isOpen);
				groupBrowser.reloadNode(groupNode);
				groupBrowser.expand(groupNode);
			} catch (ControllerException e)
			{
				NotificationPopup.showError(e);
			}
		}).show();
	}

	private class AddGroupDialog extends AbstractDialog
	{
		private BiConsumer<I18nString, Boolean> groupConsumer;
		private I18nTextField groupNameField;
		private GroupNode parentGroup;
		private CheckBox isPublic;

		public AddGroupDialog(UnityMessageSource msg, GroupNode parentGroup,
				BiConsumer<I18nString, Boolean> groupConsumer)
		{
			super(msg, msg.getMessage("AddGroupDialog.caption"));
			this.groupConsumer = groupConsumer;
			this.parentGroup = parentGroup;
			setSizeEm(30, 18);
		}

		@Override
		protected Button createConfirmButton()
		{
			Button ok = super.createConfirmButton();
			ok.addStyleName(SidebarStyles.buttonAction.toString());
			return ok;
		}
		
		@Override
		protected FormLayout getContents()
		{
			Label info = new Label(msg.getMessage("AddGroupDialog.info", parentGroup));
			info.setWidth(100, Unit.PERCENTAGE);

			groupNameField = new I18nTextField(msg,
					msg.getMessage("AddGroupDialog.groupName"));
			isPublic = new CheckBox(msg.getMessage("AddGroupDialog.public"));

			isPublic.setEnabled(parentGroup.isOpen());
			isPublic.setValue(parentGroup.isOpen());

			FormLayout main = new CompactFormLayout();
			main.addComponents(info, groupNameField, isPublic);
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			if (groupNameField.isEmpty())
			{
				groupNameField.setComponentError(new UserError(msg
						.getMessage("AddGroupDialog.emptyGroupNameError")));
				return;
			}

			groupConsumer.accept(groupNameField.getValue(), isPublic.getValue());
			close();
		}
	}

	private SingleActionHandler<GroupNode> getRenameGroupcAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.renameGroupAction"))
				.withIcon(Images.pencil.getResource())
				.withDisabledPredicate(n -> n.getPath().equals(projectPath))
				.hideIfInactive().withHandler(this::showRenameGroupDialog).build();
	}

	private void showRenameGroupDialog(Set<GroupNode> selection)
	{

		new RenameGroupDialog(msg, groupName -> {
			try
			{
				controller.updateGroupName(projectPath,
						selection.iterator().next().getPath(), groupName);
				groupBrowser.reloadNode(selection.iterator().next());
			} catch (ControllerException e)
			{
				NotificationPopup.showError(e);
			}
		}).show();
	}

	private class RenameGroupDialog extends AbstractDialog
	{
		private Consumer<I18nString> groupNameConsumer;
		private I18nTextField groupNameField;

		public RenameGroupDialog(UnityMessageSource msg,
				Consumer<I18nString> groupNameConsumer)
		{
			super(msg, msg.getMessage("RenameGroupDialog.caption"));
			this.groupNameConsumer = groupNameConsumer;
			setSizeEm(30, 18);
		}
		
		@Override
		protected Button createConfirmButton()
		{
			Button ok = super.createConfirmButton();
			ok.addStyleName(SidebarStyles.buttonAction.toString());
			return ok;
		}

		@Override
		protected FormLayout getContents()
		{
			groupNameField = new I18nTextField(msg,
					msg.getMessage("RenameGroupDialog.groupName"));
			FormLayout main = new CompactFormLayout();
			main.addComponents(groupNameField);
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			if (groupNameField.isEmpty())
			{
				groupNameField.setComponentError(new UserError(msg.getMessage(
						"RenameGroupDialog.emptyGroupNameError")));
				return;
			}

			groupNameConsumer.accept(groupNameField.getValue());
			close();
		}
	}
}
