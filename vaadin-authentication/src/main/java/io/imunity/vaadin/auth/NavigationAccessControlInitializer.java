/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import static io.imunity.vaadin.endpoint.common.SessionStorage.REDIRECT_URL_SESSION_STORAGE_KEY;
import static io.imunity.vaadin.endpoint.common.SessionStorage.SELECTED_AUTHN_STORAGE_KEY;
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
	private final boolean isJsExpression;

	static NavigationAccessControlInitializer defaultInitializer()
	{
		return new NavigationAccessControlInitializer("window.location.href", true);
	}

	static NavigationAccessControlInitializer withAfterSuccessLoginRedirect(String afterSuccessLoginRedirect)
	{
		return new NavigationAccessControlInitializer(afterSuccessLoginRedirect, false);
	}

	private NavigationAccessControlInitializer(String afterSuccessLoginRedirect, boolean isJsExpression)
	{
		navigationAccessControl = new NavigationAccessControl();
		navigationAccessControl.setLoginView(AuthenticationView.class);
		this.afterSuccessLoginRedirect = afterSuccessLoginRedirect;
		this.isJsExpression = isJsExpression;
	}

	@Override
	public void serviceInit(ServiceInitEvent serviceInitEvent) {
		serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> uiInitEvent.getUI().addBeforeEnterListener(navigationAccessControl));
		saveOriginalUrlRequestInSessionStorageBeforeAllRedirects(serviceInitEvent);
		saveOrginalSelectedAuthn(serviceInitEvent);
	}

	private void saveOriginalUrlRequestInSessionStorageBeforeAllRedirects(ServiceInitEvent serviceInitEvent)
	{
		serviceInitEvent.addIndexHtmlRequestListener(response ->
		{
			String signInCtx = response.getVaadinRequest().getParameter(URL_PARAM_CONTEXT_KEY);
			String jsValue = buildRedirectJsValue(signInCtx);
			Document document = response.getDocument();
			document.body().append("<script>window.sessionStorage.setItem("
					+ "\"" + REDIRECT_URL_SESSION_STORAGE_KEY + "\", " + jsValue + ");</script>");
		});
	}

	String buildRedirectJsValue(String signInCtx)
	{
		String queryParam = nonNull(signInCtx)
				? "?" + URL_PARAM_CONTEXT_KEY + "=" + signInCtx
				: "";
		if (isJsExpression)
		{
			return queryParam.isEmpty()
					? afterSuccessLoginRedirect
					: afterSuccessLoginRedirect + " + \"" + queryParam + "\"";
		}
		return "\"" + afterSuccessLoginRedirect + queryParam + "\"";
	}
	
	private void saveOrginalSelectedAuthn(ServiceInitEvent serviceInitEvent)
	{
		serviceInitEvent.addIndexHtmlRequestListener(response ->
		{
			String preferredIdp = response.getVaadinRequest()
					.getParameter(PreferredAuthenticationHelper.IDP_SELECT_PARAM);
			Document document = response.getDocument();
			document.body()
					.append("<script>window.sessionStorage.setItem(" + "\"" + SELECTED_AUTHN_STORAGE_KEY + "\", \""
							+ preferredIdp + "\");</script>");
		});
	}
}
