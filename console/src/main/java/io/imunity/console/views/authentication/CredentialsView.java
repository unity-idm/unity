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
@Breadcrumb(key = "WebConsoleMenu.authentication.localCredentials.breadcrumb")
@Route(value = "/credentials", layout = ConsoleMenu.class)
public class CredentialsView extends ConsoleViewComponent
{
}
