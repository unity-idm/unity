/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.web.console;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.auth.services.DefaultServicesControllerBase;
import io.imunity.vaadin.auth.services.ServiceController;
import io.imunity.vaadin.auth.services.ServiceEditor;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.endpoint.EndpointFileConfigurationManagement;
import pl.edu.icm.unity.engine.api.server.NetworkServer;
import pl.edu.icm.unity.restadm.RESTAdminEndpoint;


/**
 * Rest admin service controller. 
 * 
 * @author P.Piernik
 *
 */
@Component
class RestAdminServiceController extends DefaultServicesControllerBase implements ServiceController
{
	private final RealmsManagement realmsMan;
	private final AuthenticationFlowManagement flowsMan;
	private final AuthenticatorManagement authMan;
	private final NetworkServer networkServer;

	RestAdminServiceController(MessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
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
		return RESTAdminEndpoint.NAME;
	}

	@Override
	public ServiceEditor getEditor(SubViewSwitcher subViewSwitcher) throws EngineException
	{
		return new RestAdminServiceEditor(msg,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream().collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream().collect(Collectors.toList()),
				endpointMan.getEndpoints().stream().map(e -> e.getContextAddress())
						.collect(Collectors.toList()),
				endpointMan.getEndpoints().stream().map(e -> e.getName())
						.collect(Collectors.toList()),
				networkServer.getUsedContextPaths());
	}
}
