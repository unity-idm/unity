/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.maintenance;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;

import javax.annotation.security.PermitAll;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.maintenance.about")
@Route(value = "/about", layout = ConsoleMenu.class)
public class AboutView extends ConsoleViewComponent
{
	AboutView()
	{
		getContent().add(new Label("AboutView"));
	}
}
