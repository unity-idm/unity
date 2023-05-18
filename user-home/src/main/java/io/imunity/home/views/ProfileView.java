/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.home.views;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import javax.annotation.security.PermitAll;

@PermitAll
@RouteAlias(value = "/", layout = HomeUiMenu.class)
@Route(value = "/profile", layout = HomeUiMenu.class)
public class ProfileView extends Composite<Div>
{
}
