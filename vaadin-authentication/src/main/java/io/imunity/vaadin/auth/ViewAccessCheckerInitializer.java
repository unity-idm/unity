/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.ViewAccessChecker;
import org.jsoup.nodes.Document;

public class ViewAccessCheckerInitializer implements VaadinServiceInitListener {

	private final ViewAccessChecker viewAccessChecker;
	private final String afterSuccessLoginRedirect;


	public ViewAccessCheckerInitializer() {
		viewAccessChecker = new ViewAccessChecker();
		viewAccessChecker.setLoginView(AuthenticationView.class);
		afterSuccessLoginRedirect = "window.location.href";
	}

	public ViewAccessCheckerInitializer(String afterSuccessLoginRedirect) {
		viewAccessChecker = new ViewAccessChecker();
		viewAccessChecker.setLoginView(AuthenticationView.class);
		this.afterSuccessLoginRedirect = "\"" + afterSuccessLoginRedirect + "\"";
	}

	@Override
	public void serviceInit(ServiceInitEvent serviceInitEvent) {
		serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> uiInitEvent.getUI().addBeforeEnterListener(viewAccessChecker));
		saveOriginalUrlRequestInSessionStorageBeforeAllRedirects(serviceInitEvent);
	}

	private void saveOriginalUrlRequestInSessionStorageBeforeAllRedirects(ServiceInitEvent serviceInitEvent)
	{
		serviceInitEvent.addIndexHtmlRequestListener(response -> {
			Document document = response.getDocument();
			document.body().append("<script>window.sessionStorage.setItem(\"redirect-url\", " + afterSuccessLoginRedirect + ");</script>");
		});
	}
}
