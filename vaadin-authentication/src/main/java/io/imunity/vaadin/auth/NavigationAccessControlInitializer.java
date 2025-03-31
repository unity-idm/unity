/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import static io.imunity.vaadin.endpoint.common.SessionStorage.REDIRECT_URL_SESSION_STORAGE_KEY;
import static io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.URL_PARAM_CONTEXT_KEY;
import static java.util.Objects.nonNull;

import org.jsoup.nodes.Document;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.NavigationAccessControl;

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
			String signInCtx = response.getVaadinRequest().getParameter(URL_PARAM_CONTEXT_KEY);
			String redirect = afterSuccessLoginRedirect;
			if (nonNull(signInCtx))
			{
				redirect = afterSuccessLoginRedirect + "?" + URL_PARAM_CONTEXT_KEY + "=" + signInCtx;
			}
			Document document = response.getDocument();
			document.body().append("<script>window.sessionStorage.setItem("
					+ "\"" + REDIRECT_URL_SESSION_STORAGE_KEY + "\", " + redirect + ");</script>");
		});
	}
}
