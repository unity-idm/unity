/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.console;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.imunity.upman.UpManEndpointFactory;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.services.DefaultServicesControllerBase;
import io.imunity.vaadin.endpoint.common.api.services.ServiceController;
import io.imunity.vaadin.endpoint.common.api.services.ServiceEditor;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
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
import pl.edu.icm.unity.engine.api.server.NetworkServer;

/**
 * Upman service controller. Based on the standard web service editor
 * 
 * @author P.Piernik
 *
 */
@Component
class UpManServiceController extends DefaultServicesControllerBase implements ServiceController
{

	private final RealmsManagement realmsMan;
	private final AuthenticationFlowManagement flowsMan;
	private final AuthenticatorManagement authMan;
	private final RegistrationsManagement registrationMan;
	private final FileStorageService fileStorageService;
	private final UnityServerConfiguration serverConfig;
	private final AuthenticatorSupportService authenticatorSupportService;
	private final NetworkServer server;
	private final VaadinLogoImageLoader imageAccessService;
	private final HomeServiceLinkController homeServiceController;

	UpManServiceController(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan,
			RegistrationsManagement registrationMan, FileStorageService fileStorageService,
			UnityServerConfiguration serverConfig, AuthenticatorSupportService authenticatorSupportService,
			NetworkServer server, VaadinLogoImageLoader imageAccessService,
			HomeServiceLinkController homeServiceController,
			EndpointFileConfigurationManagement serviceFileConfigController)
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.registrationMan = registrationMan;
		this.fileStorageService = fileStorageService;
		this.serverConfig = serverConfig;
		this.authenticatorSupportService = authenticatorSupportService;
		this.server = server;
		this.imageAccessService = imageAccessService;
		this.homeServiceController = homeServiceController;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return UpManEndpointFactory.NAME;
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{

		return new UpmanServiceEditor(msg, imageAccessService, fileStorageService, serverConfig,
				realmsMan.getRealms()
						.stream()
						.map(r -> r.getName())
						.collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows()
						.stream()
						.collect(Collectors.toList()),
				authMan.getAuthenticators(null)
						.stream()
						.collect(Collectors.toList()),
				homeServiceController.getAllHomeEndpoints()
						.stream()
						.map(e -> e.getName())
						.collect(Collectors.toList()),
				registrationMan.getForms()
						.stream()
						.filter(r -> r.isPubliclyAvailable())
						.map(r -> r.getName())
						.collect(Collectors.toList()),
				endpointMan.getEndpoints()
						.stream()
						.map(e -> e.getContextAddress())
						.collect(Collectors.toList()),
				endpointMan.getEndpoints()
						.stream()
						.map(e -> e.getName())
						.collect(Collectors.toList()),
				server.getUsedContextPaths(), authenticatorSupportService);
	}

}
