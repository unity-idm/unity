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
	private final AfterSuccessLoginRedirectProvider afterSuccessLoginRedirectProvider;

	static NavigationAccessControlInitializer defaultInitializer()
	{
		return new NavigationAccessControlInitializer(new JsExpressionAfterSuccessLoginRedirectProvider());
	}

	static NavigationAccessControlInitializer withAfterSuccessLoginRedirect(String afterSuccessLoginRedirect)
	{
		return new NavigationAccessControlInitializer(new StringAfterSuccessLoginRedirectProvider(afterSuccessLoginRedirect));
	}

	private NavigationAccessControlInitializer(AfterSuccessLoginRedirectProvider afterSuccessLoginRedirectProvider)
	{
		navigationAccessControl = new NavigationAccessControl();
		navigationAccessControl.setLoginView(AuthenticationView.class);
		this.afterSuccessLoginRedirectProvider = afterSuccessLoginRedirectProvider;
	}

	@Override
	public void serviceInit(ServiceInitEvent serviceInitEvent)
	{
		serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> uiInitEvent.getUI().addBeforeEnterListener(navigationAccessControl));
		saveOriginalUrlRequestInSessionStorageBeforeAllRedirects(serviceInitEvent);
		saveOrginalSelectedAuthn(serviceInitEvent);
	}

	private void saveOriginalUrlRequestInSessionStorageBeforeAllRedirects(ServiceInitEvent serviceInitEvent)
	{
		serviceInitEvent.addIndexHtmlRequestListener(response ->
		{
			String signInCtx = response.getVaadinRequest().getParameter(URL_PARAM_CONTEXT_KEY);
			String jsValue = afterSuccessLoginRedirectProvider.get(signInCtx);
			Document document = response.getDocument();
			document.body().append("<script>window.sessionStorage.setItem("
					+ "\"" + REDIRECT_URL_SESSION_STORAGE_KEY + "\", " + jsValue + ");</script>");
		});
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

	interface AfterSuccessLoginRedirectProvider
	{
		default String signInParam(String signInCtxValue)
		{
			return nonNull(signInCtxValue)
				? "?" + URL_PARAM_CONTEXT_KEY + "=" + signInCtxValue
				: "";
		}

		String get(String signInCtx);
	}

	static class JsExpressionAfterSuccessLoginRedirectProvider implements AfterSuccessLoginRedirectProvider
	{
		private static final String JS_EXPRESSION = "window.location.href";

		@Override
		public String get(String signInCtx)
		{
			String queryParam = signInParam(signInCtx);

			return queryParam.isEmpty()
				? JS_EXPRESSION
				: JS_EXPRESSION + " + \"" + queryParam + "\"";
		}
	}

	static class StringAfterSuccessLoginRedirectProvider implements AfterSuccessLoginRedirectProvider
	{
		private final String redirectUrl;

		StringAfterSuccessLoginRedirectProvider(String redirectUrl)
		{
			this.redirectUrl = redirectUrl;
		}

		@Override
		public String get(String signInCtx)
		{
			String queryParam = signInParam(signInCtx);

			return "\"" + redirectUrl + queryParam + "\"";
		}
	}
}
