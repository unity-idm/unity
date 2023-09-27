/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.vaadin.elements.Breadcrumb;

import jakarta.annotation.security.PermitAll;

@PermitAll
@Breadcrumb(key = "AuthenticatorsComponent.test")
@Route(value = "/services/edit", layout = ConsoleMenu.class)
public class ServicesEditView extends ConsoleViewComponent
{
	ServicesEditView()
	{
		getContent().add(new Label("ServicesEditView"));
	}
}
