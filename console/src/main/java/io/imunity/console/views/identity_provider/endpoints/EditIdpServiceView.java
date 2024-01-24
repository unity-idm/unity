/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.identity_provider.endpoints;

import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.services.base.EditServiceViewBase;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.message.MessageSource;

@PermitAll
@Route(value = "/idpServices/edit", layout = ConsoleMenu.class)
public class EditIdpServiceView extends EditServiceViewBase
{
	public EditIdpServiceView(MessageSource msg, IdpServicesController controller, NotificationPresenter notificationPresenter)
	{
		super(msg, controller, IdpServicesView.class, notificationPresenter);
	}
}
