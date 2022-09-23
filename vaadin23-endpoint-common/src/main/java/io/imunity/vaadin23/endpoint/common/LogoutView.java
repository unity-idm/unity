/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

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
	public final Vaddin23WebLogoutHandler vaddin23WebLogoutHandler;

	public LogoutView(Vaddin23WebLogoutHandler vaddin23WebLogoutHandler)
	{
		this.vaddin23WebLogoutHandler = vaddin23WebLogoutHandler;
	}

	@Override
	public void afterNavigation(AfterNavigationEvent afterNavigationEvent)
	{
		UI.getCurrent().getPage().setLocation(
				VaadinServlet.getFrontendMapping()
		);
		vaddin23WebLogoutHandler.logout();
	}
}
