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
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.ProjectController;
import io.imunity.upman.UpManUI;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.project.DelegatedGroup;
import pl.edu.icm.unity.engine.api.project.GroupAuthorizationRole;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Group;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.NotificationTray;
import pl.edu.icm.unity.webui.common.SearchField;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.common.groups.MandatoryGroupSelection;
import pl.edu.icm.unity.webui.confirmations.ConfirmationInfoFormatter;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component displays members grid with hamburger menu on the top
 * 
 * @author P.Piernik
 *
 */

class GroupMembersComponent extends CustomComponent
{
	private MessageSource msg;
	private GroupMembersController controller;

	private GroupMemebersGrid groupMemebersGrid;
	private Group group;
	private String project;
	private GroupAuthorizationRole role;
	private ProjectController projectController;

	public GroupMembersComponent(MessageSource msg, GroupMembersController controller,
			ProjectController projectController, String project, ConfirmationInfoFormatter formatter)
			throws ControllerException
	{
		this.msg = msg;
		this.controller = controller;
		this.project = project;
		this.projectController = projectController;

		Map<String, String> additionalProjectAttributes = controller
				.getAdditionalAttributeNamesForProject(project);
		setSizeFull();
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);

		List<SingleActionHandler<GroupMemberEntry>> commonActions = new ArrayList<>();
		commonActions.add(getRemoveFromProjectAction());
		commonActions.add(getRemoveFromGroupAction());
		commonActions.add(getAddToGroupAction());

		List<SingleActionHandler<GroupMemberEntry>> rawActions = new ArrayList<>();
		rawActions.addAll(commonActions);
		rawActions.add(getSetProjectRoleAction());
		rawActions.add(getSetSubProjectRoleAction());

		groupMemebersGrid = new GroupMemebersGrid(msg, rawActions, additionalProjectAttributes, formatter);

		HamburgerMenu<GroupMemberEntry> hamburgerMenu = new HamburgerMenu<>();
		groupMemebersGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		hamburgerMenu.addActionHandlers(commonActions);
		hamburgerMenu.addActionHandler(getSetProjectRoleAction());
		hamburgerMenu.addActionHandler(getSetSubProjectRoleAction());
		SearchField search = FilterableGridHelper.generateSearchField(groupMemebersGrid, msg);

		HorizontalLayout menuBar = new HorizontalLayout(hamburgerMenu, search);
		menuBar.setSpacing(false);
		menuBar.setMargin(false);
		menuBar.setComponentAlignment(search, Alignment.MIDDLE_RIGHT);
		menuBar.setWidth(100, Unit.PERCENTAGE);
		main.addComponents(menuBar, groupMemebersGrid);
		main.setExpandRatio(menuBar, 0);
		main.setExpandRatio(groupMemebersGrid, 2);
	}

	private SingleActionHandler<GroupMemberEntry> getRemoveFromProjectAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage("GroupMembersComponent.removeFromProjectAction"))
				.withIcon(Images.removeFromGroup.getResource()).multiTarget()
				.withHandler(this::removeFromProject).build();
	}

	public void removeFromProject(Set<GroupMemberEntry> items)
	{
		removeFromGroup(project, items);
	}

	private SingleActionHandler<GroupMemberEntry> getRemoveFromGroupAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage("GroupMembersComponent.removeFromGroupAction"))
				.withIcon(Images.deleteFolder.getResource()).multiTarget()
				.withHandler(this::removeFromGroup).build();
	}

	public void removeFromGroup(Set<GroupMemberEntry> items)
	{
		removeFromGroup(group.toString(), items);
	}

	private void removeFromGroup(String groupFrom, Set<GroupMemberEntry> items)
	{
		if (checkIfSelfProjectOperation(groupFrom, items))
		{
			new ConfirmDialog(msg, msg.getMessage("GroupMembersComponent.confirmSelfRemoveFromProject",
					getProjectDisplayedNameSafe(project)), () -> {
						confirmedRemoveFromGroup(groupFrom, items);
						UpManUI.reloadProjects();
					}).show();
		} else
		{
			confirmedRemoveFromGroup(groupFrom, items);
			reloadMemebersGrid();
		}
	}

	private void confirmedRemoveFromGroup(String groupFrom, Set<GroupMemberEntry> items)
	{
		try
		{
			controller.removeFromGroup(project, groupFrom, items);
			NotificationTray.showSuccess(msg.getMessage("GroupMembersComponent.removed"));

		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	private SingleActionHandler<GroupMemberEntry> getAddToGroupAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage("GroupMembersComponent.addToGroupAction"))
				.withIcon(Images.add.getResource()).multiTarget()
				.withHandler(this::showAddToGroupDialog).build();
	}

	private SingleActionHandler<GroupMemberEntry> getSetProjectRoleAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage("GroupMembersComponent.setProjectRoleAction"))
				.withIcon(Images.star_open.getResource()).multiTarget()
				.withHandler(this::showSetProjectRoleDialog)
				.withDisabledPredicate(e -> !project.equals(group.toString())).hideIfInactive().build();
	}

	private SingleActionHandler<GroupMemberEntry> getSetSubProjectRoleAction()
	{
		return SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage("GroupMembersComponent.setSubProjectRoleAction"))
				.withIcon(Images.star_open.getResource()).multiTarget()
				.withHandler(this::showSetSubProjectRoleDialog)
				.withDisabledPredicate(e -> project.equals(group.toString())
						|| !group.getDelegationConfiguration().enabled
						|| !role.equals(GroupAuthorizationRole.treeManager))
				.hideIfInactive().build();
	}

	private void showSetProjectRoleDialog(Set<GroupMemberEntry> items)
	{
		new RoleSelectionDialog(msg, "RoleSelectionDialog.projectCaption", "RoleSelectionDialog.projectRole",
				role -> {
					updateProjectRole(items, role);
				},
				items.size() == 1 ? items.iterator().next().getRole() : GroupAuthorizationRole.regular)
						.show();
	}

	private void showSetSubProjectRoleDialog(Set<GroupMemberEntry> items)
	{
		new RoleSelectionDialog(msg, "RoleSelectionDialog.subprojectCaption",
				"RoleSelectionDialog.subprojectRole", role -> {
					updateRoleConfirmed(items, role);
				},
				items.size() == 1 ? items.iterator().next().getRole() : GroupAuthorizationRole.regular)
						.show();
	}

	private void updateProjectRole(Set<GroupMemberEntry> items, GroupAuthorizationRole role)
	{
		if (role.equals(GroupAuthorizationRole.regular) && checkIfSelfProjectOperation(group.toString(), items))
		{
			new ConfirmDialog(msg,
					msg.getMessage("GroupMembersComponent.confirmSelfRevokeManagerPrivileges",
							getProjectDisplayedNameSafe(project)),
					() -> {

						updateRoleConfirmed(items, role);
						UpManUI.reloadProjects();
					}).show();
		} else
		{
			updateRoleConfirmed(items, role);
		}
	}

	private void updateRoleConfirmed(Set<GroupMemberEntry> items, GroupAuthorizationRole role)
	{
		try
		{
			controller.updateRole(project, group.toString(), role, items);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		reloadMemebersGrid();
	}

	public void setGroup(Group group)
	{
		this.group = group;
		reloadMemebersGrid();
	}

	private void reloadMemebersGrid()
	{
		List<GroupMemberEntry> groupMembers = new ArrayList<>();
		try
		{
			groupMembers.addAll(controller.getGroupMembers(project, group.toString()));
			role = projectController.getProjectRole(project);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}

		groupMemebersGrid.setItems(groupMembers);

		if (group.toString().equals(project))
		{
			groupMemebersGrid.switchToProjectMode();
		} else if (group.getDelegationConfiguration().enabled)
		{
			groupMemebersGrid.switchToSubprojectMode();
		} else
		{
			groupMemebersGrid.switchToRegularSubgroupMode();
		}

	}

	private boolean checkIfSelfProjectOperation(String group, Set<GroupMemberEntry> items)
	{
		if (project.equals(group))
		{
			long managerId = InvocationContext.getCurrent().getLoginSession().getEntityId();

			for (GroupMemberEntry e : items)
			{
				if (e.getEntityId() == managerId)
					return true;
			}
		}
		return false;
	}

	private String getProjectDisplayedNameSafe(String projectPath)
	{
		String name = "";
		try
		{
			name = controller.getProjectDisplayedName(projectPath);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		return name;
	}

	private void showAddToGroupDialog(Set<GroupMemberEntry> selection)
	{

		new TargetGroupSelectionDialog(msg, group -> {
			try
			{
				controller.addToGroup(project, group, selection);
				NotificationTray.showSuccess(msg.getMessage("GroupMembersComponent.addedToGroup"));
			} catch (ControllerException e)
			{
				NotificationPopup.showError(e);
			}
			reloadMemebersGrid();

		}).show();
	}

	private List<GroupAuthorizationRole> getAvailableRoles()
	{
		if (role.equals(GroupAuthorizationRole.treeManager))
		{
			return Lists.newArrayList(GroupAuthorizationRole.regular, GroupAuthorizationRole.manager,
					GroupAuthorizationRole.treeManager);
		}

		else if (role.equals(GroupAuthorizationRole.manager))
		{
			return Lists.newArrayList(GroupAuthorizationRole.regular, GroupAuthorizationRole.manager);
		}

		return Lists.newArrayList();
	}

	private class RoleSelectionDialog extends AbstractDialog
	{
		private Consumer<GroupAuthorizationRole> selectionConsumer;
		private RadioButtonGroup<GroupAuthorizationRole> roleSelection;
		private String roleCaption;
		private GroupAuthorizationRole initRole;

		public RoleSelectionDialog(MessageSource msg, String captionKey, String roleCaption,
				Consumer<GroupAuthorizationRole> selectionConsumer, GroupAuthorizationRole initRole)
		{
			super(msg, msg.getMessage(captionKey));
			this.selectionConsumer = selectionConsumer;
			this.roleCaption = roleCaption;
			this.initRole = initRole;
			setSizeEm(38, 18);
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
			roleSelection = new RadioButtonGroup<>();
			roleSelection.setCaption(msg.getMessage(roleCaption));
			roleSelection.setItems(getAvailableRoles());
			roleSelection.setItemCaptionGenerator(
					e -> msg.getMessage("Role." + e.toString().toLowerCase()));
			roleSelection.setValue(initRole);
			FormLayout main = new CompactFormLayout();
			main.addComponents(roleSelection);
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			selectionConsumer.accept(roleSelection.getValue());
			close();
		}
	}

	private class TargetGroupSelectionDialog extends AbstractDialog
	{
		private Consumer<String> selectionConsumer;
		private MandatoryGroupSelection groupSelection;

		public TargetGroupSelectionDialog(MessageSource msg, Consumer<String> selectionConsumer)
		{
			super(msg, msg.getMessage("AddToGroupDialog.caption"));
			this.selectionConsumer = selectionConsumer;
			setSizeEm(38, 18);
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
			Label info = new Label(msg.getMessage("AddToGroupDialog.info"));
			info.setWidth(100, Unit.PERCENTAGE);

			List<DelegatedGroup> groups = new ArrayList<>();
			try
			{
				groups.addAll(controller.getProjectGroups(project));
			} catch (ControllerException e)
			{
				NotificationPopup.showError(e);
			}

			groupSelection = new MandatoryGroupSelection(msg);
			groupSelection.setCaption(msg.getMessage("AddToGroupDialog.selectGroup"));
			groupSelection.setItems(groups.stream().map(dg -> {
				Group g = new Group(dg.path);
				g.setDisplayedName(new I18nString(dg.displayedName));
				return g;
			}).collect(Collectors.toList()));

			FormLayout main = new CompactFormLayout();
			main.addComponents(info, groupSelection);
			main.setSizeFull();
			return main;
		}

		@Override
		protected void onConfirm()
		{
			selectionConsumer.accept(groupSelection.getValue().group.toString());
			close();
		}
	}

	public static interface MemberFilter extends SerializablePredicate<GroupMemberEntry>
	{
	}

}
