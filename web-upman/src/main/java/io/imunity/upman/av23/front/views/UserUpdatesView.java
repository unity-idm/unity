/*
 * Copyright (c) 2020 Bixbit s.c. All rights reserved.
 * See LICENSE file for licensing information.
 */

package io.imunity.upman.av23.front.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.Route;
import io.imunity.upman.av23.front.components.UnityViewComponent;

@Route(value = "/user-updates", layout = UpManMenu.class)
public class UserUpdatesView extends UnityViewComponent
{
	public UserUpdatesView() {
		getContent().add(new H1("User updates"));

	}
}
