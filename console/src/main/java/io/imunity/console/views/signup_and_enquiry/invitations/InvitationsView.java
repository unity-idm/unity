/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.invitations;

import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.ShowViewActionLayoutFactory;
import io.imunity.console.views.signup_and_enquiry.invitations.viewer.MainInvitationViewer;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.IllegalFormTypeException;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.signup_and_enquiry.invitations", parent = "WebConsoleMenu.signup_and_enquiry")
@Route(value = "/invitations", layout = ConsoleMenu.class)
public class InvitationsView extends ConsoleViewComponent 
{
	private final MessageSource msg;
	private final InvitationsService invitationService;
	private final NotificationPresenter notificationPresenter;

	public InvitationsView(MessageSource msg, InvitationsService invitationService, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.invitationService = invitationService;
		this.notificationPresenter = notificationPresenter;
		init();
	}

	private void init()
	{
		InvitationsGrid invitationsGrid = new InvitationsGrid(msg, invitationService, notificationPresenter);
		
		MainInvitationViewer viewer;
		try
		{
			viewer = invitationService.getViewer();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
			return;
		}

		invitationsGrid.addValueChangeListener(invitation -> {
			try
			{
				viewer.setInput(invitation);
			} catch (IllegalFormTypeException e)
			{
				notificationPresenter.showError("", e.getCause()
						.getMessage());
				return;
			}
		});

		SplitLayout splitLayout  = new SplitLayout(Orientation.VERTICAL);
		splitLayout.addToPrimary(invitationsGrid);
		splitLayout.addToSecondary(viewer);
		splitLayout.setSizeFull();
		splitLayout.setSplitterPosition(40);
		
		List<Button> buttons = getButtonsBar();
		
		
		VerticalLayout wrapper = new VerticalLayout(ShowViewActionLayoutFactory.buildTopButtonsBar(buttons.toArray(new Button[buttons.size()])));
		wrapper.setPadding(true);
		wrapper.setMargin(false);
		getContent().add(wrapper);
		getContent().add(splitLayout);
		getContent().setSizeFull();
	}
	protected List<Button> getButtonsBar()
	{
		Button newProfile = ShowViewActionLayoutFactory.build4AddAction(msg, e -> UI.getCurrent()
				.navigate(NewInvitationView.class));
		return Arrays.asList(newProfile);
	}
	
}
