/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services;

import org.springframework.stereotype.Component;

import io.imunity.webconsole.services.base.ServiceControllerBase;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.webui.console.services.ServiceControllersRegistry;

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
			ServiceControllersRegistry controllersRegistry)
	{
		super(msg, endpointMan, controllersRegistry);
	}
}
