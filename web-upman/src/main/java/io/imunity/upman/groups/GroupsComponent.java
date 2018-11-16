/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component displays groups in tree and simple hamburger menu on the top
 * 
 * @author P.Piernik
 *
 */

public class GroupsComponent extends VerticalLayout
{
	private UnityMessageSource msg;
	private GroupsController controller;
	private GroupsTree groupBrowser;
	private String rootPath;

	public GroupsComponent(UnityMessageSource msg, GroupsController controller,
			String rootGroup) throws ControllerException
	{
		this.msg = msg;
		this.controller = controller;
		this.rootPath = rootGroup;

		List<SingleActionHandler<GroupNode>> rawActions = new ArrayList<>();
		rawActions.add(getDeleteGroupAction());
		rawActions.add(getAddToGroupAction());
		rawActions.add(getMakePublicAction(true));
		rawActions.add(getMakePrivateAction(true));
		rawActions.add(getRenameGroupcAction());

		groupBrowser = new GroupsTree(msg, controller, rawActions, rootGroup);
		HamburgerMenu<GroupNode> hamburgerMenu = new HamburgerMenu<>();
		groupBrowser.addSelectionListener(hamburgerMenu.getSelectionListener());

		// hamburgerMenu.addActionHandler(getDeleteGroupAction());
		hamburgerMenu.addActionHandler(getMakePublicAction(false));
		hamburgerMenu.addActionHandler(getMakePrivateAction(false));
		hamburgerMenu.addActionHandler(getExpandAllAction());
		hamburgerMenu.addActionHandler(getCollapseAllAction());

		HorizontalLayout menuBar = new HorizontalLayout();
		menuBar.setSpacing(false);
		// TODO remove space, add styles
		Label space = new Label();
		space.setWidth(9, Unit.PIXELS);
		menuBar.addComponents(space, hamburgerMenu);
		addComponents(menuBar, groupBrowser);
	}

	private SingleActionHandler<GroupNode> getMakePrivateAction(boolean hideIfInactive)
	{
		SingleActionHandler<GroupNode> handler = SingleActionHandler
				.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.makePrivateAction"))
				.withIcon(Images.padlock_lock.getResource()).multiTarget()
				.withHandler(this::makePrivate).build();
		handler.setHideIfInactive(hideIfInactive);
		return handler;
	}

	private void makePrivate(Set<GroupNode> items)
	{
		try
		{
			for (GroupNode group : items)
			{

				controller.setPrivateGroupAccess(group.getPath());
				groupBrowser.reloadNode(group);
			}
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private SingleActionHandler<GroupNode> getMakePublicAction(boolean hideIfInactive)
	{
		SingleActionHandler<GroupNode> handler = SingleActionHandler
				.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.makePublicAction"))
				.withIcon(Images.padlock_unlock.getResource()).multiTarget()
				.withHandler(this::makePublic).build();
		handler.setHideIfInactive(hideIfInactive);
		return handler;
	}

	private void makePublic(Set<GroupNode> items)
	{
		try
		{
			for (GroupNode group : items)
			{

				controller.setPublicGroupAccess(group.getPath());
				groupBrowser.reloadNode(group);
			}
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private SingleActionHandler<GroupNode> getExpandAllAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.expandAllAction"))
				.withIcon(Images.expand.getResource()).dontRequireTarget()
				.withHandler(e -> groupBrowser.expandAll()).build();
	}

	private SingleActionHandler<GroupNode> getCollapseAllAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.collapseAllAction"))
				.withIcon(Images.expand.getResource()).dontRequireTarget()
				.withHandler(e -> groupBrowser.collapseAll()).build();
	}

	private SingleActionHandler<GroupNode> getDeleteGroupAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.deleteGroupAction"))
				.withIcon(Images.removeFromGroup.getResource())
				.withDisabledPredicate(n -> n.getPath().equals(rootPath))
				.hideIfInactive().withHandler(this::confirmDelete).build();
	}

	private void confirmDelete(Set<GroupNode> items)
	{

		final GroupNode node = items.iterator().next();
		new ConfirmDialog(msg,
				msg.getMessage("RemoveGroupDialog.confirmDelete", node.toString()),
				() -> deleteGroup(node)

		).show();

	}

	private void deleteGroup(GroupNode group)
	{
		try
		{

			controller.deleteGroup(group.getPath());
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

	private void showAddGroupDialog(Set<GroupNode> selection)
	{

		GroupNode item = selection.iterator().next();
		new AddGroupDialog(msg, item, group -> {
			try
			{
				controller.addGroup(group);
				groupBrowser.reloadNode(item);
				groupBrowser.expand(item);
			} catch (ControllerException e)
			{
				NotificationPopup.showError(e);
			}
		}).show();
	}

	private class AddGroupDialog extends AbstractDialog
	{
		private Consumer<Group> groupConsumer;
		private TextField groupNameField;
		private GroupNode parentGroup;
		private CheckBox isPublic;

		public AddGroupDialog(UnityMessageSource msg, GroupNode parentGroup,
				Consumer<Group> groupConsumer)
		{
			super(msg, msg.getMessage("AddGroupDialog.caption"));
			this.groupConsumer = groupConsumer;
			this.parentGroup = parentGroup;
			setSizeEm(30, 18);
		}

		@Override
		protected FormLayout getContents()
		{
			Label info = new Label(msg.getMessage("AddGroupDialog.info", parentGroup));
			info.setWidth(100, Unit.PERCENTAGE);

			groupNameField = new TextField(msg.getMessage("AddGroupDialog.groupName"));
			isPublic = new CheckBox(msg.getMessage("AddGroupDialog.public"));

			FormLayout main = new CompactFormLayout();
			main.addComponents(info, groupNameField, isPublic);
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			groupConsumer.accept(new Group(
					parentGroup.getPath() + "/" + groupNameField.getValue()));
			close();
		}
	}

	private SingleActionHandler<GroupNode> getRenameGroupcAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.renameGroupAction"))
				.withIcon(Images.pencil.getResource())
				.withHandler(this::showRenameGroupDialog).build();
	}

	private void showRenameGroupDialog(Set<GroupNode> selection)
	{

		new RenameGroupDialog(msg, groupName -> {
			try
			{
				controller.updateGroupName(selection.iterator().next().getPath(),
						groupName);
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
			groupNameConsumer.accept(groupNameField.getValue());
			close();
		}
	}

}
