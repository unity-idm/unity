/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.signup_and_enquiry.requests;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.signup_and_enquiry.EnquiryResponsesChangedEvent;
import io.imunity.console.views.signup_and_enquiry.RegistrationRequestsChangedEvent;
import io.imunity.vaadin.elements.Breadcrumb;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.bus.EventsBus;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;

/**
 * Lists all registration and enquiry requests.
 * 
 * @author P.Piernik
 *
 */
@PermitAll
@Breadcrumb(key = "WebConsoleMenu.signup_and_enquiry.requests", parent = "WebConsoleMenu.signup_and_enquiry")
@Route(value = "/requests", layout = ConsoleMenu.class)
public class RequestsView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final RequestsService controller;
	private final RequestProcessingPanel requestPanel;
	private final NotificationPresenter notificationPresenter;

	@Autowired
	RequestsView(MessageSource msg, RequestsService controller, RequestProcessingPanel requestPanel,
			NotificationPresenter notificationPresenter)

	{
		this.msg = msg;
		this.controller = controller;
		this.requestPanel = requestPanel;
		this.notificationPresenter = notificationPresenter;
		init();
	}

	private void init()
	{
		RequestsGrid requestGrid = new RequestsGrid(msg, controller, notificationPresenter);
		EventsBus eventBus = WebSession.getCurrent()
				.getEventBus();
		eventBus.addListener(event -> requestGrid.refresh(), RegistrationRequestsChangedEvent.class);
		eventBus.addListener(event -> requestGrid.refresh(), EnquiryResponsesChangedEvent.class);

		requestGrid.addValueChangeListener(new RequestSelectionListener()
		{
			@Override
			public void registrationChanged(RegistrationRequestState request)
			{
				requestPanel.setVisible(true);
				requestPanel.setRequest(request);
			}

			@Override
			public void enquiryChanged(EnquiryResponseState request)
			{
				requestPanel.setVisible(true);
				requestPanel.setRequest(request);
			}

			@Override
			public void deselected()
			{
				requestPanel.setVisible(false);
			}
		});

		SplitLayout splitLayout = new SplitLayout(Orientation.VERTICAL);
		splitLayout.addToPrimary(requestGrid);
		splitLayout.addToSecondary(requestPanel);
		splitLayout.setSizeFull();
		splitLayout.setSplitterPosition(40);

		VerticalLayout main = new VerticalLayout();
		main.add(splitLayout);
		main.setSizeFull();
		main.setMargin(false);
		main.setPadding(false);

		getContent().add(main);
		getContent().setSizeFull();
	}
}
