/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.NotificationTray;
import pl.edu.icm.unity.webui.common.SearchField;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component displays invitations grid with hamburger menu on the top
 * 
 * @author P.Piernik
 *
 */
public class ProjectInvitationsComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private ProjectInvitationsController controller;

	private ProjectInvitationsGrid invitationsGrid;
	private String project;

	public ProjectInvitationsComponent(UnityMessageSource msg, ProjectInvitationsController controller, String project)
	{
		this.msg = msg;
		this.controller = controller;
		this.project = project;

		setSizeFull();
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);

		List<SingleActionHandler<ProjectInvitationEntry>> commonActions = new ArrayList<>();
		commonActions.add(getRemoveInvitationAction());
		commonActions.add(getResendInvitationAction());

		invitationsGrid = new ProjectInvitationsGrid(msg, commonActions);

		HamburgerMenu<ProjectInvitationEntry> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addStyleNames(SidebarStyles.indentSmall.toString());
		hamburgerMenu.addStyleName(SidebarStyles.sidebar.toString());
		invitationsGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		hamburgerMenu.addActionHandlers(commonActions);

		SearchField search = FilterableGridHelper.generateSearchField(invitationsGrid, msg);
		HorizontalLayout menuBar = new HorizontalLayout(hamburgerMenu, search);
		menuBar.setComponentAlignment(search, Alignment.MIDDLE_RIGHT);
		menuBar.setWidth(100, Unit.PERCENTAGE);
		
		reload();
		main.addComponents(menuBar, invitationsGrid);
		main.setExpandRatio(menuBar , 0);
		main.setExpandRatio(invitationsGrid, 2);
	}

	private SingleActionHandler<ProjectInvitationEntry> getRemoveInvitationAction()
	{
		return SingleActionHandler.builder(ProjectInvitationEntry.class)
				.withCaption(msg.getMessage("InvitationsComponent.removeInvitationAction"))
				.withIcon(Images.trash.getResource()).multiTarget().withHandler(this::deleteInvitation)
				.build();
	}

	private void deleteInvitation(Set<ProjectInvitationEntry> items)
	{
		try
		{
			controller.removeInvitations(project, items);
			NotificationTray.showSuccess(msg.getMessage("InvitationsComponent.removed"));

		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		reload();
	}

	private SingleActionHandler<ProjectInvitationEntry> getResendInvitationAction()
	{
		return SingleActionHandler.builder(ProjectInvitationEntry.class)
				.withCaption(msg.getMessage("InvitationsComponent.resendInvitationAction"))
				.withIcon(Images.envelope.getResource()).multiTarget()
				.withHandler(this::resendInvitation).build();
	}

	private void resendInvitation(Set<ProjectInvitationEntry> items)
	{
		try
		{
			controller.resendInvitations(project, items);
			NotificationTray.showSuccess(msg.getMessage("InvitationsComponent.sent"));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		reload();
	}

	public void reload()
	{
		List<ProjectInvitationEntry> invitations = new ArrayList<>();
		try
		{
			invitations.addAll(controller.getInvitations(project));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
			
		invitationsGrid.setValue(invitations);
	}
}
