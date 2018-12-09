/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.userupdates;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.VerticalLayout;

import io.imunity.upman.common.ProjectAttributeController;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Component displays update requests grid with hamburger menu on the top
 * 
 * @author P.Piernik
 *
 */
public class UpdateRequestsComponent extends CustomComponent
{
	private UnityMessageSource msg;
	private UpdateRequestsController controller;

	private UpdateRequestsGrid updateRequestGrid;
	private String project;

	public UpdateRequestsComponent(UnityMessageSource msg, UpdateRequestsController controller,
			ProjectAttributeController attrController, String project) throws ControllerException
	{
		this.msg = msg;
		this.controller = controller;
		this.project = project;
		Map<String, String> additionalProjectAttributes = attrController
				.getAdditionalAttributeNamesForProject(project);

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.setSpacing(false);
		setCompositionRoot(main);

		List<SingleActionHandler<UpdateRequestEntry>> commonActions = new ArrayList<>();
		commonActions.add(getAcceptRequestAction());
		commonActions.add(getDeclineRequestAction());

		updateRequestGrid = new UpdateRequestsGrid(msg, commonActions, additionalProjectAttributes);

		HamburgerMenu<UpdateRequestEntry> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addStyleNames(SidebarStyles.indentSmall.toString());
		hamburgerMenu.addStyleName(SidebarStyles.sidebar.toString());
		updateRequestGrid.addSelectionListener(hamburgerMenu.getSelectionListener());

		hamburgerMenu.addActionHandlers(commonActions);

		HorizontalLayout menuBar = new HorizontalLayout(hamburgerMenu);
		
		Link selfSingUpForm = new Link(msg.getMessage("UpdateRequestsComponent.selfSignUpForm"), null);	
		Link updateForm = new Link(msg.getMessage("UpdateRequestsComponent.updateForm"), null);
	
		Label space = new Label();
		
		main.addComponents(selfSingUpForm, updateForm, space , menuBar, updateRequestGrid);
		reloadRequestsGrid();

	}

	private SingleActionHandler<UpdateRequestEntry> getAcceptRequestAction()
	{
		return SingleActionHandler.builder(UpdateRequestEntry.class)
				.withCaption(msg.getMessage("UpdateRequestsComponent.acceptRequestAction"))
				.withIcon(Images.ok.getResource()).multiTarget().withHandler(this::acceptRequest)
				.build();
	}

	public void acceptRequest(Set<UpdateRequestEntry> items)
	{

	}

	private SingleActionHandler<UpdateRequestEntry> getDeclineRequestAction()
	{
		return SingleActionHandler.builder(UpdateRequestEntry.class)
				.withCaption(msg.getMessage("UpdateRequestsComponent.declineRequestAction"))
				.withIcon(Images.reject.getResource()).multiTarget().withHandler(this::declineRequest)
				.build();
	}

	public void declineRequest(Set<UpdateRequestEntry> items)
	{

	}

	private void reloadRequestsGrid()
	{
		List<UpdateRequestEntry> requests = new ArrayList<>();
		try
		{
			requests.addAll(controller.getUpdateRequests(project));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(e);
		}

		updateRequestGrid.setValue(requests);
	}
}
