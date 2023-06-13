/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.signupAndEnquiry.requests;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.signupAndEnquiry.SignupAndEnquiryNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryResponseState;
import pl.edu.icm.unity.base.registration.RegistrationRequestState;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestChangedEvent;
import pl.edu.icm.unity.webui.sessionscope.WebSessionComponent;

/**
 * Lists all registration and enquiry requests.
 * 
 * @author P.Piernik
 *
 */
@WebSessionComponent
class RequestsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "Requests";

	private MessageSource msg;
	private RequestsController controller;
	private RequestProcessingPanel requestPanel;

	@Autowired
	RequestsView(MessageSource msg, RequestsController controller, RequestProcessingPanel requestPanel)
	{
		this.msg = msg;
		this.controller = controller;
		this.requestPanel = requestPanel;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		RequestsGrid requestGrid = new RequestsGrid(msg, controller);
		EventsBus eventBus = WebSession.getCurrent().getEventBus();
		eventBus.addListener(e -> requestGrid.refresh(), RegistrationRequestChangedEvent.class);
		eventBus.addListener(e -> requestGrid.refresh(), EnquiryResponseChangedEvent.class);
	
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

		Panel viewerPanel = new Panel();
		viewerPanel.setContent(requestPanel);
		viewerPanel.setSizeFull();
		viewerPanel.setStyleName(Styles.vPanelBorderless.toString());

		CompositeSplitPanel splitPanel = new CompositeSplitPanel(true, false, requestGrid, viewerPanel, 50);
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
		return msg.getMessage("WebConsoleMenu.signupAndEnquiry.requests");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class RequestsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public RequestsNavigationInfoProvider(MessageSource msg,
				 ObjectFactory<RequestsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(SignupAndEnquiryNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.signupAndEnquiry.requests"))
					.withIcon(Images.user_card.getResource())
					.withPosition(20).build());

		}
	}
}
