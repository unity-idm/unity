/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api.services;

import java.util.stream.Collectors;

import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.server.NetworkServer;

/**
 * Controller for service which support web authentication
 * 
 * @author P.Piernik
 *
 */
public class WebServiceControllerBase extends DefaultServicesControllerBase implements ServiceController
{
	private final RealmsManagement realmsMan;
	private final AuthenticationFlowManagement flowsMan;
	private final AuthenticatorManagement authMan;
	private final RegistrationsManagement registrationMan;
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final NetworkServer networkServer;
	private final EndpointTypeDescription type;
	private final String defaultMainTheme;
	private final VaadinLogoImageLoader imageAccessService;

	public WebServiceControllerBase(EndpointTypeDescription type, MessageSource msg,
			EndpointManagement endpointMan, RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
			RegistrationsManagement registrationMan, URIAccessService uriAccessService,
			VaadinLogoImageLoader imageAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService, NetworkServer networkServer,
			EndpointFileConfigurationManagement serviceFileConfigController)
	{
		this(type, msg, endpointMan, realmsMan, flowsMan, authMan, registrationMan, imageAccessService,
				fileStorageService, serverConfig, authenticatorSupportService, networkServer, serviceFileConfigController, 
				null);
	}

	public WebServiceControllerBase(EndpointTypeDescription type, MessageSource msg,
			EndpointManagement endpointMan, RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
			RegistrationsManagement registrationMan,
			VaadinLogoImageLoader imageAccessService,
			FileStorageService fileStorageService, UnityServerConfiguration serverConfig,
			AuthenticatorSupportService authenticatorSupportService, NetworkServer networkServer,
			EndpointFileConfigurationManagement serviceFileConfigController, String defaultMainTheme)
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.registrationMan = registrationMan;
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
		return new WebServiceEditor(type, msg, imageAccessService, fileStorageService, serverConfig,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
				registrationMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				endpointMan.getEndpoints().stream().map(e -> e.getContextAddress()).collect(
						Collectors.toList()), endpointMan.getEndpoints().stream().map(e -> e.getName()).collect(
								Collectors.toList()) , networkServer.getUsedContextPaths(),
				authenticatorSupportService, defaultMainTheme);
	}
}
