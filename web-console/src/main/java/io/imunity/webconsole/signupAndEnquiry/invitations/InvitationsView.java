/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.invitations;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.signupAndEnquiry.SignupAndEnquiryNavigationInfoProvider;
import io.imunity.webconsole.signupAndEnquiry.invitations.viewer.MainInvitationViewer;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all invitations
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class InvitationsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Invitations";

	private final MessageSource msg;
	private final InvitationsController controller;

	private  InvitationsGrid invitationsGrid;

	@Autowired
	InvitationsView(MessageSource msg, InvitationsController controller)
	{
		this.msg = msg;
		this.controller = controller;

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper.buildTopButtonsBar(StandardButtonsHelper
				.build4AddAction(msg, e -> NavigationHelper.goToView(NewInvitationView.VIEW_NAME)));

		invitationsGrid = new InvitationsGrid(msg, controller);

		MainInvitationViewer viewer;
		try
		{
			viewer = controller.getViewer();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}

		invitationsGrid.addValueChangeListener(invitation -> {
			try
			{
				viewer.setInput(invitation);
			} catch (IllegalFormTypeException e)
			{
				NotificationPopup.showError(msg, "Invalid form type", e);
				return;
			}
		});

		Panel viewerPanel = new Panel();
		viewerPanel.setContent(viewer);
		viewerPanel.setSizeFull();
		viewerPanel.setStyleName(Styles.vPanelBorderless.toString());

		VerticalLayout gridWrapper = new VerticalLayout();
		gridWrapper.setMargin(false);
		gridWrapper.setSpacing(true);
		gridWrapper.addComponent(buttonsBar);
		gridWrapper.setExpandRatio(buttonsBar, 0);
		gridWrapper.addComponent(invitationsGrid);
		gridWrapper.setExpandRatio(invitationsGrid, 2);
		gridWrapper.setSizeFull();

		CompositeSplitPanel splitPanel = new CompositeSplitPanel(true, false, gridWrapper, viewerPanel, 50);
		splitPanel.setSizeFull();

		VerticalLayout main = new VerticalLayout();
		main.addComponent(splitPanel);
		main.setSizeFull();
		main.setMargin(false);
		setCompositionRoot(main);
		setSizeFull();
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.signupAndEnquiry.invitations");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class InvitationsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = VIEW_NAME;
		
		@Autowired
		public InvitationsNavigationInfoProvider(MessageSource msg, ObjectFactory<InvitationsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(SignupAndEnquiryNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.signupAndEnquiry.invitations"))
					.withIcon(Images.taxi.getResource())
					.withPosition(30).build());

		}
	}
}
