/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.jsoup.nodes.Document;

class BaseVaadinServiceInitListener implements VaadinServiceInitListener
{
	@Override
	public void serviceInit(ServiceInitEvent serviceInitEvent)
	{
		saveOriginalUrlRequestInSessionStorageBeforeAllRedirects(serviceInitEvent);
	}

	private void saveOriginalUrlRequestInSessionStorageBeforeAllRedirects(ServiceInitEvent serviceInitEvent)
	{
		serviceInitEvent.addIndexHtmlRequestListener(response ->
		{
			Document document = response.getDocument();
			document.body().append("<script>window.sessionStorage.setItem(\"redirect-url\", window.location.href);</script>");
		});
	}
}
