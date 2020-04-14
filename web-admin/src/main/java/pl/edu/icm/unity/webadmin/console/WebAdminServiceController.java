/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.console;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webadmin.WebAdminEndpointFactory;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.console.services.WebServiceControllerBase;

/**
 * Webadmin service controller. Based on the standard web service editor
 * 
 * @author P.Piernik
 *
 */
@Component
class WebAdminServiceController extends WebServiceControllerBase
{
	WebAdminServiceController(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
			RegistrationsManagement registrationMan, URIAccessService uriAccessService,
			ImageAccessService imageAccessService, FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService, NetworkServer networkServer)
	{
		super(WebAdminEndpointFactory.TYPE, msg, endpointMan, realmsMan, flowsMan, authMan, registrationMan,
				uriAccessService, imageAccessService, fileStorageService, serverConfig, 
				authenticatorSupportService, networkServer);
	}
}
