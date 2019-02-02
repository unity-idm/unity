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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.vaadin.server.SerializablePredicate;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.UpManUI;
import io.imunity.upman.utils.UpManGridHelper;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
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
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
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
	private UnityMessageSource msg;
	private GroupMembersController controller;
	
	private GroupMemebersGrid groupMemebersGrid;
	private String group;
	private String project;

	public GroupMembersComponent(UnityMessageSource msg, GroupMembersController controller, String project, ConfirmationInfoFormatter formatter)
			throws ControllerException
	{
		this.msg = msg;
		this.controller = controller;
		this.project = project;
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
		rawActions.add(getAddManagerPrivilegesAction(true));
		rawActions.add(getRevokeManagerPrivilegesAction(true, s -> false));

		groupMemebersGrid = new GroupMemebersGrid(msg, rawActions, additionalProjectAttributes, formatter);

		HamburgerMenu<GroupMemberEntry> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addStyleNames(SidebarStyles.indentSmall.toString());
		hamburgerMenu.addStyleName(SidebarStyles.sidebar.toString());
		groupMemebersGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		hamburgerMenu.addActionHandlers(commonActions);
		hamburgerMenu.addActionHandler(getAddManagerPrivilegesAction(false));
		hamburgerMenu.addActionHandler(
				getRevokeManagerPrivilegesAction(false, s -> checkIfAllManagersSelected(s)));
		TextField search = UpManGridHelper.generateSearchField(groupMemebersGrid, msg);
		
		HorizontalLayout menuBar = new HorizontalLayout(hamburgerMenu, search);
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
		removeFromGroup(group, items);
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
			NotificationTray.showSuccess( msg.getMessage("GroupMembersComponent.removed"));

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

	private SingleActionHandler<GroupMemberEntry> getAddManagerPrivilegesAction(boolean hideIfInactive)
	{
		SingleActionHandler<GroupMemberEntry> handler = SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage("GroupMembersComponent.addManagerPrivilegesAction"))
				.withIcon(Images.trending_up.getResource()).multiTarget()
				.withHandler(this::addManagerPrivileges)
				.withDisabledPredicate(e -> !e.getRole().equals(GroupAuthorizationRole.regular))
				.build();
		handler.setHideIfInactive(hideIfInactive);
		return handler;
	}

	public void addManagerPrivileges(Set<GroupMemberEntry> items)
	{
		try
		{
			controller.addManagerPrivileges(project, items);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		reloadMemebersGrid();
	}

	private SingleActionHandler<GroupMemberEntry> getRevokeManagerPrivilegesAction(boolean hideIfInactive,
			Predicate<Set<GroupMemberEntry>> disabledCompositePredicate)
	{
		SingleActionHandler<GroupMemberEntry> handler = SingleActionHandler.builder(GroupMemberEntry.class)
				.withCaption(msg.getMessage("GroupMembersComponent.revokeManagerPrivilegesAction"))
				.withIcon(Images.trending_down.getResource()).multiTarget()
				.withHandler(this::revokeManagerPrivileges)
				.withDisabledPredicate(e -> !e.getRole().equals(GroupAuthorizationRole.manager)
						|| groupMemebersGrid.getManagersCount() < 2)
				.withDisabledCompositePredicate(disabledCompositePredicate).build();
		handler.setHideIfInactive(hideIfInactive);
		return handler;
	}

	private boolean checkIfAllManagersSelected(Set<GroupMemberEntry> items)
	{
		int selectedManagerCount = 0;
		for (GroupMemberEntry e : items)
		{
			if (e.getRole().equals(GroupAuthorizationRole.manager))
			{
				selectedManagerCount++;
			}
		}
		if (selectedManagerCount == groupMemebersGrid.getManagersCount())
		{
			return true;
		}
		return false;
	}

	public void revokeManagerPrivileges(Set<GroupMemberEntry> items)
	{
		if (checkIfSelfProjectOperation(project, items))
		{
			new ConfirmDialog(msg,
					msg.getMessage("GroupMembersComponent.confirmSelfRevokeManagerPrivileges",
							getProjectDisplayedNameSafe(project)),
					() -> {

						confirmedRevokeManagerPrivileges(items);
						UpManUI.reloadProjects();
					}).show();

		} else
		{
			confirmedRevokeManagerPrivileges(items);
			reloadMemebersGrid();
		}
	}

	private void confirmedRevokeManagerPrivileges(Set<GroupMemberEntry> items)
	{
		try
		{
			controller.revokeManagerPrivileges(project, items);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
	}

	public void setGroup(String group)
	{
		this.group = group;
		reloadMemebersGrid();
	}

	private void reloadMemebersGrid()
	{
		List<GroupMemberEntry> groupMembers = new ArrayList<>();
		try
		{
			groupMembers.addAll(controller.getGroupMembers(project, group));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}

		groupMemebersGrid.setValue(groupMembers);
	}

	private boolean checkIfSelfProjectOperation(String group, Set<GroupMemberEntry> items)
	{
		boolean selfOperation = false;
		if (project.equals(group))
		{
			long managerId = InvocationContext.getCurrent().getLoginSession().getEntityId();

			for (GroupMemberEntry e : items)
			{
				if (e.getEntityId() == managerId)
					selfOperation = true;
			}
		}
		return selfOperation;
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

	private class TargetGroupSelectionDialog extends AbstractDialog
	{
		private Consumer<String> selectionConsumer;
		private MandatoryGroupSelection groupSelection;

		public TargetGroupSelectionDialog(UnityMessageSource msg, Consumer<String> selectionConsumer)
		{
			super(msg, msg.getMessage("AddToGroupDialog.caption"));
			this.selectionConsumer = selectionConsumer;
			setSizeEm(38, 18);
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
