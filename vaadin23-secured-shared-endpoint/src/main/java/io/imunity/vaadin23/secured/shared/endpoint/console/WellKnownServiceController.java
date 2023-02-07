/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.secured.shared.endpoint.console;

import io.imunity.vaadin23.secured.shared.endpoint.SecuredSharedEndpointFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServicesControllerBase;
import pl.edu.icm.unity.webui.console.services.ServiceController;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;

import java.util.stream.Collectors;

@Component
class WellKnownServiceController extends DefaultServicesControllerBase implements ServiceController
{
	private RealmsManagement realmsMan;
	private AuthenticationFlowManagement flowsMan;
	private AuthenticatorManagement authMan;
	private NetworkServer networkServer;

	public WellKnownServiceController(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
	                                  AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan, NetworkServer networkServer,
	                                  UnityServerConfiguration unityServerConfiguration, ImageAccessService imageAccessService,
	                                  EndpointFileConfigurationManagement serviceFileConfigController)
	{
		super(msg, endpointMan, serviceFileConfigController);
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
		this.networkServer = networkServer;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return SecuredSharedEndpointFactory.NAME;
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{
		return new WellKnownServiceEditor(msg,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
				endpointMan.getEndpoints().stream().map(e -> e.getContextAddress()).collect(Collectors.toList()),
				networkServer.getUsedContextPaths());
	}

}
