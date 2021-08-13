/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.facilities;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.UnityViewWithSandbox;
import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.AuthenticationNavigationInfoProvider;
import io.imunity.webconsole.authentication.authenticators.AuthenticatorsComponent;
import io.imunity.webconsole.authentication.authenticators.AuthenticatorsController;
import io.imunity.webconsole.authentication.flows.AuthenticationFlowsComponent;
import io.imunity.webconsole.authentication.flows.AuthenticationFlowsController;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Lists all authenticators and flows
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class AuthenticationFacilitiesView extends CustomComponent implements UnityViewWithSandbox
{
	public static final String VIEW_NAME = "AuthenticationSetup";

	private MessageSource msg;
	private AuthenticationFlowsController flowsMan;
	private AuthenticatorsController authnMan;
	private SandboxAuthnRouter sandBoxRouter;

	@Autowired
	AuthenticationFacilitiesView(MessageSource msg, AuthenticationFlowsController flowsMan,
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
		main.addComponent(new AuthenticatorsComponent(msg, authnMan, sandBoxRouter));
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
		return msg.getMessage("WebConsoleMenu.authentication.facilities");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class AuthenticationFacilitiesNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = AuthenticationFacilitiesView.VIEW_NAME;

		@Autowired
		public AuthenticationFacilitiesNavigationInfoProvider(MessageSource msg,
				ObjectFactory<AuthenticationFacilitiesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(AuthenticationNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.authentication.facilities"))
					.withIcon(Images.sign_in.getResource())
					.withPosition(10).build());

		}
	}

	@Override
	public void setSandboxRouter(SandboxAuthnRouter router)
	{
		sandBoxRouter = router;
		
	}
}
