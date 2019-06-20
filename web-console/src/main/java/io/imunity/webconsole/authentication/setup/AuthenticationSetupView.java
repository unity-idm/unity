/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.setup;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.AuthenticationNavigationInfoProvider;
import io.imunity.webconsole.authentication.authenticators.AuthenticatorsComponent;
import io.imunity.webconsole.authentication.authenticators.AuthenticatorsController;
import io.imunity.webconsole.authentication.flows.AuthenticationFlowsComponent;
import io.imunity.webconsole.authentication.flows.AuthenticationFlowsController;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Lists all authenticators and flows
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class AuthenticationSetupView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "AuthenticationSetup";

	private UnityMessageSource msg;
	private AuthenticationFlowsController flowsMan;
	private AuthenticatorsController authnMan;

	@Autowired
	AuthenticationSetupView(UnityMessageSource msg, AuthenticationFlowsController flowsMan,
			AuthenticatorsController authnMan)
	{
		this.msg = msg;
		this.flowsMan = flowsMan;
		this.authnMan = authnMan;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		main.addComponent(new AuthenticatorsComponent(msg, authnMan));
		main.addComponent(new Label());
		main.addComponent(new AuthenticationFlowsComponent(msg, flowsMan));
		main.addComponent(new Label());
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);
		setCompositionRoot(main);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.authentication.setup");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class AuthenticationSetupNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public AuthenticationSetupNavigationInfoProvider(UnityMessageSource msg,
				AuthenticationNavigationInfoProvider parent,
				ObjectFactory<AuthenticationSetupView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.authentication.setup"))
					.withIcon(Images.exit.getResource())
					.withPosition(10).build());

		}
	}
}
