/*
 * Copyright (c) 2020 Bixbit s.c. All rights reserved.
 * See LICENSE file for licensing information.
 */

package io.imunity.upman.av23.front.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.Route;
import io.imunity.upman.av23.front.components.UnityViewComponent;

@Route(value = "/invitations", layout = UpManMenu.class)
public class InvitationsView extends UnityViewComponent
{

	public InvitationsView() {
		getContent().add(new H1("Invitations"));
	}

	@Override
	public void loadData()
	{

	}
}
