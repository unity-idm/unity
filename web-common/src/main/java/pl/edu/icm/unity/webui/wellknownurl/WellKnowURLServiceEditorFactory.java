/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.wellknownurl;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;

import pl.edu.icm.unity.webui.authn.endpoints.ServiceEditor;
import pl.edu.icm.unity.webui.authn.endpoints.ServiceEndpointEditorFactory;

/**
 * 
 * @author P.Piernik
 *
 */
@Component
public class WellKnowURLServiceEditorFactory implements ServiceEndpointEditorFactory
{

	private UnityMessageSource msg;
	private RealmsManagement realmsMan;
	private AuthenticationFlowManagement flowsMan;
	private AuthenticatorManagement authMan;

	@Autowired
	public WellKnowURLServiceEditorFactory(UnityMessageSource msg, RealmsManagement realmsMan,
			AuthenticationFlowManagement flowsMan, AuthenticatorManagement authMan)
	{
		this.msg = msg;
		this.realmsMan = realmsMan;
		this.flowsMan = flowsMan;
		this.authMan = authMan;
	}

	@Override
	public String getSupportedEndpointType()
	{
		return WellKnownURLEndpointFactory.NAME;
	}

	@Override
	public ServiceEditor createInstance() throws EngineException
	{
		return new WellKnownServiceEditor(msg,
				realmsMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toList()),
				flowsMan.getAuthenticationFlows().stream()
						.collect(Collectors.toList()),
				authMan.getAuthenticators(null).stream()
						.collect(Collectors.toList()));
	}
}
