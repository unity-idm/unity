/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.authentication;

import com.vaadin.flow.router.Route;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.Breadcrumb;

import jakarta.annotation.security.PermitAll;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.authentication.inputTranslation")
@Route(value = "/remote-data-profiles", layout = ConsoleMenu.class)
public class RemoteDataProfilesView extends ConsoleViewComponent
{
}
