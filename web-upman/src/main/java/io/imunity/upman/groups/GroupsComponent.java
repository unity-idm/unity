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

import com.google.common.collect.Lists;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.engine.api.project.SubprojectGroupDelegationConfiguration;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.GroupDelegationConfiguration;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component displays groups in tree grid with hamburger menu on the top
 * 
 * @author P.Piernik
 *
 */

class GroupsComponent extends CustomComponent
{
	private MessageSource msg;
	private GroupsController controller;
	private GroupsTree groupBrowser;
	private DelegatedGroup project;
	private GroupAuthorizationRole role;

	public GroupsComponent(MessageSource msg, GroupsController controller, GroupAuthorizationRole role,
			DelegatedGroup project) throws ControllerException
	{
		this.msg = msg;
		this.controller = controller;
		this.project = project;
		this.role = role;

		List<SingleActionHandler<GroupNode>> rawActions = new ArrayList<>();
		rawActions.add(getDeleteGroupAction());
		rawActions.add(getAddToGroupAction());
		rawActions.add(getMakePublicAction());
		rawActions.add(getMakePrivateAction());
		rawActions.add(getRenameGroupcAction());
		rawActions.add(getDelegateGroupcAction());

		groupBrowser = new GroupsTree(msg, controller, rawActions, project.path);
		HamburgerMenu<GroupNode> hamburgerMenu = new HamburgerMenu<>();

		groupBrowser.addSelectionListener(hamburgerMenu.getSelectionListener());

		hamburgerMenu.addActionHandler(getExpandAllAction());
		hamburgerMenu.addActionHandler(getCollapseAllAction());

		HorizontalLayout menuBar = new HorizontalLayout(hamburgerMenu);
		VerticalLayout vl = new VerticalLayout();
		vl.setMargin(false);
		vl.setSpacing(false);
		vl.addComponents(menuBar, groupBrowser);
		vl.setExpandRatio(menuBar, 0);
		vl.setExpandRatio(groupBrowser, 2);
		vl.setSizeFull();
		setCompositionRoot(vl);
		setSizeFull();
	}

	private SingleActionHandler<GroupNode> getMakePrivateAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.makePrivateAction"))
				.withIcon(Images.padlock_lock.getResource()).withDisabledPredicate(n -> {
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
				.withIcon(Images.padlock_unlock.getResource()).withDisabledPredicate(n -> {
					boolean disabled = n.isOpen();
					if (n.parent != null)
					{
						disabled = disabled || !n.parent.isOpen();
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

			controller.setGroupAccessMode(project.path, groupNode.getPath(), isPublic);
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
				.withDisabledPredicate(n -> n.getPath().equals(project.path)).hideIfInactive()
				.withHandler(this::confirmDelete).build();
	}

	private void confirmDelete(Set<GroupNode> items)
	{

		if (items.isEmpty())
			return;

		GroupNode groupNode = items.iterator().next();
		new ConfirmDialog(msg, msg.getMessage("RemoveGroupDialog.confirmDelete", groupNode.toString()),
				() -> deleteGroup(groupNode)

		).show();
	}

	private void deleteGroup(GroupNode group)
	{
		try
		{

			controller.deleteGroup(project.path, group.getPath());
			groupBrowser.reloadNode(group.parent);

		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}

	}

	private SingleActionHandler<GroupNode> getAddToGroupAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.addGroupAction"))
				.withIcon(Images.add.getResource()).multiTarget().withHandler(this::showAddGroupDialog)
				.build();
	}

	private void showAddGroupDialog(Set<GroupNode> items)
	{

		if (items.isEmpty())
			return;

		GroupNode groupNode = items.iterator().next();
		new AddGroupDialog(msg, groupNode, project.delegationConfiguration, checkIfAdminCanCreateSubproject(groupNode.getPath()),
				(groupNameWithAccessMode, groupDelegationConfig) -> {
					try
					{
						String newGroupPath = controller.addGroup(project.path,
								groupNode.getPath(), groupNameWithAccessMode);
						if (groupDelegationConfig != null)
						{
							controller.setGroupDelegationConfiguration(project.path, newGroupPath,
									groupDelegationConfig);
						}
								
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
		private BiConsumer<GroupWithAccessMode, SubprojectGroupDelegationConfiguration> groupConsumer;
		private I18nTextField groupNameField;
		private GroupNode parentGroup;
		private CheckBox isPublic;
		private DelagateGroupDialogContent groupDelegationContent;

		public AddGroupDialog(MessageSource msg, GroupNode parentGroup,
				GroupDelegationConfiguration projectConfig, boolean showSubprojectConfig,
				BiConsumer<GroupWithAccessMode, SubprojectGroupDelegationConfiguration> groupConsumer)
		{
			super(msg, msg.getMessage("AddGroupDialog.caption"));
			this.groupConsumer = groupConsumer;
			this.parentGroup = parentGroup;
			this.groupDelegationContent = showSubprojectConfig
					? new DelagateGroupDialogContent(msg, projectConfig,
							new GroupDelegationConfiguration(false))
					: null;
			setSizeEm(50, 25);
		}

		@Override
		protected Button createConfirmButton()
		{
			Button ok = super.createConfirmButton();
			ok.addStyleName(Styles.buttonAction.toString());
			return ok;
		}

		@Override
		protected FormLayout getContents()
		{
			Label info = new Label(msg.getMessage("AddGroupDialog.info", parentGroup));
			info.setWidth(100, Unit.PERCENTAGE);

			groupNameField = new I18nTextField(msg, msg.getMessage("AddGroupDialog.groupName"));
			isPublic = new CheckBox(msg.getMessage("AddGroupDialog.public"));

			isPublic.setEnabled(parentGroup.isOpen());
			isPublic.setValue(parentGroup.isOpen());

			FormLayout main = new CompactFormLayout();
			main.addComponents(info, groupNameField, isPublic);
			if (groupDelegationContent != null)
			{
				groupDelegationContent.getComponents().forEach(main::addComponent);
			}
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			if (groupNameField.isEmpty())
			{
				groupNameField.setComponentError(
						new UserError(msg.getMessage("AddGroupDialog.emptyGroupNameError")));
				return;
			}

			groupConsumer.accept(new GroupWithAccessMode(groupNameField.getValue(), isPublic.getValue()),
					groupDelegationContent == null ? null : groupDelegationContent.getValue());
			close();
		}
	}

	private SingleActionHandler<GroupNode> getRenameGroupcAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.renameGroupAction"))
				.withIcon(Images.pencil.getResource())
				.withDisabledPredicate(n -> n.getPath().equals(project.path)).hideIfInactive()
				.withHandler(this::showRenameGroupDialog).build();
	}

	private SingleActionHandler<GroupNode> getDelegateGroupcAction()
	{
		return SingleActionHandler.builder(GroupNode.class)
				.withCaption(msg.getMessage("GroupsComponent.delegateGroupAction"))
				.withIcon(Images.workplace.getResource())
				.withDisabledPredicate(n -> !checkIfAdminCanChangeDelegationConfig(n.getPath()))
				.hideIfInactive().withHandler(this::showDelegateGroupDialog).build();
	}

	private boolean checkIfAdminCanChangeDelegationConfig(String path)
	{
		if (path.equals(project.path))
			return false;
		return checkIfAdminCanCreateSubproject(path);
	}
	
	private boolean checkIfAdminCanCreateSubproject(String path)
	{
		
		if (!project.delegationConfiguration.enabled || !project.delegationConfiguration.enableSubprojects)
			return false;
		if (!role.equals(GroupAuthorizationRole.projectsAdmin))
			return false;
		return true;
	}

	private void showDelegateGroupDialog(Set<GroupNode> selection)
	{

		new DelegateGroupDialog(msg, project.delegationConfiguration,
				selection.iterator().next().group.delegationConfiguration, groupDelegationConfig -> {
					try
					{
						controller.setGroupDelegationConfiguration(project.path,
								selection.iterator().next().getPath(),
								groupDelegationConfig);
						groupBrowser.reloadNode(selection.iterator().next());
					} catch (ControllerException e)
					{
						NotificationPopup.showError(e);
					}
				}).show();
	}

	private class DelegateGroupDialog extends AbstractDialog
	{
		private Consumer<SubprojectGroupDelegationConfiguration> groupDelegateConsumer;
		private DelagateGroupDialogContent content;

		public DelegateGroupDialog(MessageSource msg, GroupDelegationConfiguration projectConfig,
				GroupDelegationConfiguration groupConfig,
				Consumer<SubprojectGroupDelegationConfiguration> groupDelegateConsumer)
		{
			super(msg, msg.getMessage("SubprojectDialog.caption"));
			this.groupDelegateConsumer = groupDelegateConsumer;
			this.content = new DelagateGroupDialogContent(msg, projectConfig, groupConfig);
			setSizeEm(60, 18);
		}

		@Override
		protected Button createConfirmButton()
		{
			Button ok = super.createConfirmButton();
			ok.addStyleName(Styles.buttonAction.toString());
			return ok;
		}

		@Override
		protected FormLayout getContents()
		{
			FormLayout main = new CompactFormLayout();
			content.getComponents().forEach(main::addComponent);
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			groupDelegateConsumer.accept(content.getValue());
			close();
		}
	}

	private void showRenameGroupDialog(Set<GroupNode> selection)
	{

		new RenameGroupDialog(msg, groupName -> {
			try
			{
				controller.updateGroupName(project.path, selection.iterator().next().getPath(),
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

		public RenameGroupDialog(MessageSource msg, Consumer<I18nString> groupNameConsumer)
		{
			super(msg, msg.getMessage("RenameGroupDialog.caption"));
			this.groupNameConsumer = groupNameConsumer;
			setSizeEm(30, 18);
		}

		@Override
		protected Button createConfirmButton()
		{
			Button ok = super.createConfirmButton();
			ok.addStyleName(Styles.buttonAction.toString());
			return ok;
		}

		@Override
		protected FormLayout getContents()
		{
			groupNameField = new I18nTextField(msg, msg.getMessage("RenameGroupDialog.groupName"));
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
				groupNameField.setComponentError(
						new UserError(msg.getMessage("RenameGroupDialog.emptyGroupNameError")));
				return;
			}

			groupNameConsumer.accept(groupNameField.getValue());
			close();
		}
	}

	private static class DelagateGroupDialogContent
	{
		private CheckBox enableDelegation;
		private CheckBox enableSubprojects;
		private TextField logoUrl;

		DelagateGroupDialogContent(MessageSource msg, GroupDelegationConfiguration projectConfig,
				GroupDelegationConfiguration groupConfig)
		{
			enableDelegation = new CheckBox(msg.getMessage("SubprojectDialog.enableDelegationCaption"));
			enableDelegation.setDescription(msg.getMessage("SubprojectDialog.enableDelegationDescription"));
			enableDelegation.addValueChangeListener(e -> {
				enableEdit(e.getValue());
			});

			enableSubprojects = new CheckBox(msg.getMessage("SubprojectDialog.enableSubprojects"));
			enableSubprojects.setValue(groupConfig.enableSubprojects);
			enableSubprojects.setVisible(projectConfig.enableSubprojects || groupConfig.enableSubprojects);

			logoUrl = new TextField(msg.getMessage("SubprojectDialog.logoUrlCaption"));
			logoUrl.setWidth(100, Unit.PERCENTAGE);
			if (groupConfig.logoUrl != null)
				logoUrl.setValue(groupConfig.logoUrl);

			enableEdit(false);
			enableDelegation.setValue(groupConfig.enabled);
		}

		private void enableEdit(boolean enabled)
		{
			logoUrl.setEnabled(enabled);
			enableSubprojects.setEnabled(enabled);
		}

		ArrayList<Component> getComponents()
		{
			return Lists.newArrayList(enableDelegation, logoUrl, enableSubprojects);
		}

		SubprojectGroupDelegationConfiguration getValue()
		{
			return new SubprojectGroupDelegationConfiguration(enableDelegation.getValue(),
					enableSubprojects.getValue(), logoUrl.getValue());
		}

	}

}
