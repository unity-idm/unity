/*
 * Copyright (c) 2020 Bixbit s.c. All rights reserved.
 * See LICENSE file for licensing information.
 */

package io.imunity.upman.av23.front.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import io.imunity.upman.av23.front.components.UnityViewComponent;

@Route(value = "/members", layout = UpManMenu.class)
@RouteAlias(value = "/", layout = UpManMenu.class)
public class MembersView extends UnityViewComponent
{

	public MembersView() {
		getContent().add(new H1("Members"));
	}
}
