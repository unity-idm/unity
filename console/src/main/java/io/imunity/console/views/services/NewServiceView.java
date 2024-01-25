/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.services;

import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.services.base.NewServiceViewBase;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;

@PermitAll
@Route(value = "/services/new", layout = ConsoleMenu.class)
public class NewServiceView extends NewServiceViewBase
{
	public NewServiceView(MessageSource msg, ServicesController controller, NotificationPresenter notificationPresenter)
	{
		super(msg, controller, ServicesView.class, notificationPresenter);
	}
}
