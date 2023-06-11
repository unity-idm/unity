/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.idprovider.endpoints;

import org.springframework.stereotype.Component;

import io.imunity.webconsole.services.base.ServiceControllerBase;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.webui.console.services.idp.IdpServiceControllersRegistry;

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
			IdpServiceControllersRegistry controllersRegistry)
	{
		super(msg, endpointMan, controllersRegistry);
	}
}
