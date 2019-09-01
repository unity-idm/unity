/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.web.service;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EndpointManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.restadm.RESTAdminEndpoint;
import pl.edu.icm.unity.webui.authn.services.DefaultServicesControllerBase;
import pl.edu.icm.unity.webui.authn.services.ServiceController;
import pl.edu.icm.unity.webui.authn.services.ServiceEditor;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * Rest admin service controller. 
 * 
 * @author P.Piernik
 *
 */
@Component
class RestAdminServiceController extends DefaultServicesControllerBase implements ServiceController
{
	private RealmsManagement realmsMan;
	private AuthenticationFlowManagement flowsMan;
	private AuthenticatorManagement authMan;

	RestAdminServiceController(UnityMessageSource msg, EndpointManagement endpointMan, RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan)
	{
		super(msg, endpointMan);
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
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
						.collect(Collectors.toList()));
	}
}
