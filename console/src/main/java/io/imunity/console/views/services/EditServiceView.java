/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.services;

import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.services.base.EditServiceViewBase;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;

@PermitAll
@Route(value = "/services/edit", layout = ConsoleMenu.class)
public class EditServiceView extends EditServiceViewBase
{

	public EditServiceView(MessageSource msg, ServicesController controller)
	{
		super(msg, controller, ServicesView.class);
	}

}
