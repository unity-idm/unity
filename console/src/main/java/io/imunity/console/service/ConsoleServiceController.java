/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.service;

import io.imunity.console.ConsoleEndpointFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webui.common.ThemeConstans;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.WebServiceControllerBase;


@Component
class ConsoleServiceController extends WebServiceControllerBase
{
	ConsoleServiceController(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
							 AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
							 RegistrationsManagement registrationMan, URIAccessService uriAccessService,
							 ImageAccessService imageAccessService,
							 FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
							 AuthenticatorSupportService authenticatorSupportService, NetworkServer networkServer,
							 EndpointFileConfigurationManagement serviceFileConfigController)
	{
		super(ConsoleEndpointFactory.TYPE, msg, endpointMan, realmsMan, flowsMan, authMan, registrationMan,
				uriAccessService, imageAccessService, fileStorageService, serverConfig, 
				authenticatorSupportService, networkServer, serviceFileConfigController, 
				ThemeConstans.unityTheme);
	}
}
