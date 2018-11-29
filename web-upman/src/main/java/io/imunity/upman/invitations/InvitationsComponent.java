/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.invitations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component displays invitations grid with hamburger menu on the top
 * 
 * @author P.Piernik
 *
 */
public class InvitationsComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private InvitationsController controller;

	private InvitationsGrid invitationsGrid;
	private String project;

	public InvitationsComponent(UnityMessageSource msg, InvitationsController controller, String project)
	{
		this.msg = msg;
		this.controller = controller;
		this.project = project;

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);

		List<SingleActionHandler<InvitationEntry>> commonActions = new ArrayList<>();
		commonActions.add(getDeleteInvitationAction());
		commonActions.add(getResendInvitationAction());

		invitationsGrid = new InvitationsGrid(msg, commonActions);

		HamburgerMenu<InvitationEntry> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addStyleNames(SidebarStyles.indentSmall.toString());
		hamburgerMenu.addStyleName(SidebarStyles.sidebar.toString());
		invitationsGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		hamburgerMenu.addActionHandlers(commonActions);

		HorizontalLayout menuBar = new HorizontalLayout(hamburgerMenu);
		reload();
		main.addComponents(menuBar, invitationsGrid);

	}

	private SingleActionHandler<InvitationEntry> getDeleteInvitationAction()
	{
		return SingleActionHandler.builder(InvitationEntry.class)
				.withCaption(msg.getMessage("InvitationsComponent.deleteInvitationAction"))
				.withIcon(Images.trash.getResource()).multiTarget().withHandler(this::deleteInvitation)
				.build();
	}

	private void deleteInvitation(Set<InvitationEntry> items)
	{
		try
		{
			controller.deleteInvitations(project, items);

		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		reload();
	}

	private SingleActionHandler<InvitationEntry> getResendInvitationAction()
	{
		return SingleActionHandler.builder(InvitationEntry.class)
				.withCaption(msg.getMessage("InvitationsComponent.resendInvitationAction"))
				.withIcon(Images.envelope.getResource()).multiTarget()
				.withHandler(this::resendInvitation).build();
	}

	private void resendInvitation(Set<InvitationEntry> items)
	{
		try
		{
			controller.resendInvitations(project, items);

		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}
		reload();
	}

	public void reload()
	{
		List<InvitationEntry> groupMembers = new ArrayList<>();
		try
		{
			groupMembers.addAll(controller.getInvitations(project));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}

		invitationsGrid.setValue(groupMembers);
	}

}
