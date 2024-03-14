/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.identity_provider.endpoints;

import org.springframework.stereotype.Component;

import io.imunity.console.views.services.base.ServiceControllerBase;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.auth.services.idp.IdpServiceControllersRegistry;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;

/**
 * Controller for IDP services
 * 
 * @author P.Piernik
 *
 */
@Component
class IdpServicesController extends ServiceControllerBase
{
	IdpServicesController(MessageSource msg, EndpointManagement endpointMan,
			IdpServiceControllersRegistry controllersRegistry, NotificationPresenter notificationPresenter)
	{
		super(msg, endpointMan, controllersRegistry, notificationPresenter);
	}
}
