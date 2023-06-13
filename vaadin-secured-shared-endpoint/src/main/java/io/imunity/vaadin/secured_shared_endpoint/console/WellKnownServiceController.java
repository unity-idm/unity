/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.secured_shared_endpoint.console;

import io.imunity.vaadin.secured_shared_endpoint.SecuredSharedEndpointFactory;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.console.services.DefaultServicesControllerBase;
import pl.edu.icm.unity.webui.console.services.ServiceController;
import pl.edu.icm.unity.webui.console.services.ServiceEditor;

import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
class WellKnownServiceController extends DefaultServicesControllerBase implements ServiceController
{
	private final RealmsManagement realmsMan;
	private final AuthenticationFlowManagement flowsMan;
	private final AuthenticatorManagement authMan;
	private final NetworkServer networkServer;

	public WellKnownServiceController(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
	                                  AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan, NetworkServer networkServer,
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
				realmsMan.getRealms().stream().map(DescribedObjectROImpl::getName).collect(Collectors.toList()),
				new ArrayList<>(flowsMan.getAuthenticationFlows()),
				new ArrayList<>(authMan.getAuthenticators(null)),
				endpointMan.getEndpoints().stream().map(Endpoint::getContextAddress).collect(Collectors.toList()),
				networkServer.getUsedContextPaths());
	}

}
