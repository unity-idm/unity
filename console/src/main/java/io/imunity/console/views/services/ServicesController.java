/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.services;

import org.springframework.stereotype.Component;

import io.imunity.console.views.services.base.ServiceControllerBase;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.services.ServiceControllersRegistry;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;

/**
 * Controller for standard services
 * 
 * @author P.Piernik
 *
 */
@Component
class ServicesController extends ServiceControllerBase
{
	ServicesController(MessageSource msg, EndpointManagement endpointMan,
			ServiceControllersRegistry controllersRegistry, NotificationPresenter notificationPresenter)
	{
		super(msg, endpointMan, controllersRegistry, notificationPresenter);
	}
}
