/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.services;

import org.springframework.stereotype.Component;

import io.imunity.webconsole.services.base.ServiceControllerBase;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.services.ServiceControllersRegistry;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
class ServicesController extends ServiceControllerBase
{			
	ServicesController(UnityMessageSource msg, EndpointManagement endpointMan,
			 ServiceControllersRegistry controllersRegistry)
	{
		super(msg, endpointMan, controllersRegistry);
	}
}
