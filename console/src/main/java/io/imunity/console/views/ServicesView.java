/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import io.imunity.console.ConsoleMenu;
import io.imunity.vaadin.elements.Breadcrumb;

import javax.annotation.security.PermitAll;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.services")
@Route(value = "/services", layout = ConsoleMenu.class)
public class ServicesView extends ConsoleViewComponent
{
	ServicesView()
	{
		getContent().add(new Label("ServicesView"));
		RouterLink routerLink = new RouterLink(ServicesEditView.class);
		routerLink.add(new Label("edit"));
		getContent().add(routerLink);
	}
}
