/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import io.imunity.console.ConsoleMenu;
import io.imunity.vaadin.elements.Breadcrumb;

import javax.annotation.security.PermitAll;

@PermitAll
@RouteAlias(value = "/", layout = ConsoleMenu.class)
@Breadcrumb(key = "WebConsoleMenu.directoryBrowser")
@Route(value = "/directory-browser", layout = ConsoleMenu.class)
public class DirectoryBrowserView extends ConsoleViewComponent
{
	DirectoryBrowserView()
	{
		getContent().add(new Label("DirectoryBrowserView"));
	}
}
