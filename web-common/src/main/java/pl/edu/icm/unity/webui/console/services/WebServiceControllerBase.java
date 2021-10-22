/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.console.services;

import java.util.stream.Collectors;

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
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * Controller for service which support web authentication
 * 
 * @author P.Piernik
 *
 */
public class WebServiceControllerBase extends DefaultServicesControllerBase implements ServiceController
{
	private RealmsManagement realmsMan;
	private AuthenticationFlowManagement flowsMan;
	private AuthenticatorManagement authMan;
	private RegistrationsManagement registrationMan;
	private URIAccessService uriAccessService;
	private FileStorageService fileStorageService;
	private UnityServerConfiguration serverConfig;
	private AuthenticatorSupportService authenticatorSupportService;
	private NetworkServer networkServer;
	private EndpointTypeDescription type;
	private String defaultMainTheme;
	private ImageAccessService imageAccessService;

	public WebServiceControllerBase(EndpointTypeDescription type, MessageSource msg,
			EndpointManagement endpointMan, RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
			RegistrationsManagement registrationMan, URIAccessService uriAccessService,
			ImageAccessService imageAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService, NetworkServer networkServer,
			ServiceFileConfigurationController serviceFileConfigController)
	{
		this(type, msg, endpointMan, realmsMan, flowsMan, authMan, registrationMan, uriAccessService, imageAccessService,
				fileStorageService, serverConfig, authenticatorSupportService, networkServer, serviceFileConfigController, 
				null);
	}

	public WebServiceControllerBase(EndpointTypeDescription type, MessageSource msg,
			EndpointManagement endpointMan, RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
			RegistrationsManagement registrationMan, URIAccessService uriAccessService,
			ImageAccessService imageAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService, NetworkServer networkServer,
			ServiceFileConfigurationController serviceFileConfigController, String defaultMainTheme)
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.registrationMan = registrationMan;
		this.uriAccessService = uriAccessService;
		this.imageAccessService = imageAccessService;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.type = type;
		this.defaultMainTheme = defaultMainTheme;
		this.networkServer = networkServer;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return type.getName();
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{
		return new WebServiceEditor(type, msg, uriAccessService, imageAccessService, fileStorageService, serverConfig,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
				registrationMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				endpointMan.getEndpoints().stream().map(e -> e.getContextAddress()).collect(
						Collectors.toList()), networkServer.getUsedContextPaths(),
				authenticatorSupportService, defaultMainTheme);
	}
}
