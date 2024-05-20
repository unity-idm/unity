/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.NavigationAccessControl;

import io.imunity.vaadin.endpoint.common.SessionStorage;

import static io.imunity.vaadin.endpoint.common.SessionStorage.REDIRECT_URL_SESSION_STORAGE_KEY;

import org.jsoup.nodes.Document;

public class NavigationAccessControlInitializer implements VaadinServiceInitListener
{
	private final NavigationAccessControl navigationAccessControl;
	private final String afterSuccessLoginRedirect;

	static NavigationAccessControlInitializer defaultInitializer()
	{
		return new NavigationAccessControlInitializer("window.location.href");
	}

	static NavigationAccessControlInitializer withAfterSuccessLoginRedirect(String afterSuccessLoginRedirect)
	{
		return new NavigationAccessControlInitializer("\"" + afterSuccessLoginRedirect + "\"");
	}

	private NavigationAccessControlInitializer(String afterSuccessLoginRedirect)
	{
		navigationAccessControl = new NavigationAccessControl();
		navigationAccessControl.setLoginView(AuthenticationView.class);
		this.afterSuccessLoginRedirect = afterSuccessLoginRedirect;
	}

	@Override
	public void serviceInit(ServiceInitEvent serviceInitEvent) {
		serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> uiInitEvent.getUI().addBeforeEnterListener(navigationAccessControl));
		saveOriginalUrlRequestInSessionStorageBeforeAllRedirects(serviceInitEvent);
	}

	private void saveOriginalUrlRequestInSessionStorageBeforeAllRedirects(ServiceInitEvent serviceInitEvent)
	{
		serviceInitEvent.addIndexHtmlRequestListener(response -> 
		{
			Document document = response.getDocument();
			document.body().append("<script>window.sessionStorage.setItem("
					+ "\"" + REDIRECT_URL_SESSION_STORAGE_KEY + "\", " + afterSuccessLoginRedirect + ");</script>");
		});
	}
}
