/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServlet;

@Route(value = LogoutView.LOGOUT_URL)
class LogoutView extends Div implements AfterNavigationObserver
{
	public static final String LOGOUT_URL = "logout";
	public final VaddinWebLogoutHandler vaddinWebLogoutHandler;

	public LogoutView(VaddinWebLogoutHandler vaddinWebLogoutHandler)
	{
		this.vaddinWebLogoutHandler = vaddinWebLogoutHandler;
	}

	@Override
	public void afterNavigation(AfterNavigationEvent afterNavigationEvent)
	{
		UI.getCurrent().getPage().setLocation(
				VaadinServlet.getFrontendMapping()
		);
		vaddinWebLogoutHandler.logout();
	}
}
